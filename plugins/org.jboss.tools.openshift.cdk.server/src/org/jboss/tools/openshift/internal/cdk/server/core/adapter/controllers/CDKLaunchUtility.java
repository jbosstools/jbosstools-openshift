/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import static org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants.ATTR_ARGS;
import static org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants.ENVIRONMENT_VARS_KEY;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.util.ArgsUtil;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.MinishiftBinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.VagrantBinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.CDKServerUtility;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;

public class CDKLaunchUtility {
	public static final int STARTUP = 0;
	public static final int SHUTDOWN = 1;
	public static final int OTHER = 2;
	
	
	
	private static final String[] getUserPass(IServer server) {
		final CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		String user = cdkServer.getUsername();
		String pass = null;
		try {
			pass = cdkServer.getPassword();
		} catch (UsernameChangedException uce) {
			pass = uce.getPassword();
			user = uce.getUser();
		}
		return new String[] { user, pass };
	}

	private static Map<String, String> getEnvironment(IServer s, 
			ILaunchConfiguration startupConfig, boolean skipCredentials)
			throws CoreException {
		final CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		// Set the environment flag
		boolean passCredentials = cdkServer.passCredentials();
		if (passCredentials && !skipCredentials) {
			String[] userPass = getUserPass(s);
			String userName = userPass[0];
			String pass = userPass[1];
			Map<String, String> existingEnvironment = startupConfig.getAttribute(ENVIRONMENT_VARS_KEY,
					(Map<String, String>) null);
			if (existingEnvironment == null) {
				existingEnvironment = new HashMap<>();
			}

			if (userName == null) {
				// This is an error situation and the user should be made aware
				throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID,
						"The credentials for " + s.getName()
								+ " are invalid. No username found. Please open your server editor "
								+ "and set your access.redhat.com credentials."));
			}

			HashMap<String, String> env = new HashMap<>(existingEnvironment);
			String userKey = cdkServer.getUserEnvironmentKey();
			env.put(userKey, userName);

			String passKey = cdkServer.getPasswordEnvironmentKey();
			if (pass == null) {
				// This is an error situation and the user should be made aware
				throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID,
						"The credentials for " + s.getName() + " are invalid. No password found for username "
								+ cdkServer.getUsername()
								+ " for the access.redhat.com domain. Please open your server editor "
								+ "set your access.redhat.com credentials."));
			}
			if (passKey != null && pass != null)
				env.put(passKey, pass);
			return env;
		} else {
			return startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String, String>) null);
		}
	}

	public ILaunchConfigurationWorkingCopy createExternalToolsLaunch(IServer s, String args, String launchConfigName,
			ILaunchConfiguration startupConfig, String commandLoc, boolean skipCredentials) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = findLaunchConfig(s, launchConfigName);
		wc.setAttributes(startupConfig.getAttributes());
		wc.setAttribute(ATTR_ARGS, args);
		// Set the environment flag
		Map<String, String> env = getEnvironment(s, startupConfig, skipCredentials);
		wc.setAttribute(ENVIRONMENT_VARS_KEY, env);

		if (commandLoc != null) {
			wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, commandLoc);
			String cmdFolder = new Path(commandLoc).removeLastSegments(1).toOSString();
			CommandLocationLookupStrategy.get().ensureOnPath(env, cmdFolder);
		}

		return wc;
	}

	private ILaunchConfigurationWorkingCopy findLaunchConfig(IServer s, String launchName) throws CoreException {
		return ExternalLaunchUtil.findExternalToolsLaunchConfig(s, launchName);
	}

	/*
	 * The following methods are for dealing with raw calls to vagrant, not using
	 * launch configurations at all.
	 */

	/*
	 * Convert a string/string hashmap into an array of string environment variables
	 * as required by java.lang.Runtime This will super-impose the provided
	 * environment variables ON TOP OF the existing environment in eclipse, as users
	 * may not know *all* environment variables that need to be set, or to do so may
	 * be tedious.
	 */
	public static String[] convertEnvironment(Map<String, String> env) {
		if (env == null || env.size() == 0)
			return null;

		// Create a new map based on pre-existing environment of Eclipse
		Map<String, String> original = new HashMap<>(System.getenv());

		// Add additions or changes to environment on top of existing
		original.putAll(env);

		// Convert the combined map into a form that can be used to launch process
		ArrayList<String> ret = new ArrayList<>();
		Iterator<String> it = original.keySet().iterator();
		String working = null;
		while (it.hasNext()) {
			working = it.next();
			ret.add(working + "=" + original.get(working)); //$NON-NLS-1$
		}
		return ret.toArray(new String[ret.size()]);
	}

	public static String[] callMachineReadable(String rootCommand, String[] args, File vagrantDir,
			Map<String, String> env) throws IOException, CommandTimeoutException {
		return call(rootCommand, args, vagrantDir, env, 30000, false);
	}

	private static void ensureCommandOnPath(String rootCommand, Map<String, String> env) {
		CommandLocationLookupStrategy.get().ensureOnPath(env, new Path(rootCommand).removeLastSegments(1).toOSString());
	}

	public Process callInteractive(IServer s, String args, String launchConfigName, boolean skipCredentials) throws CoreException, IOException {
		return callInteractive(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
	}

	public Process callInteractive(IServer s, String args, 
			String launchConfigName, ILaunchConfiguration startupConfig, boolean skipCredentials)
			throws CoreException, IOException {
		Map<String, String> env = getEnvironment(s, startupConfig, skipCredentials);
		String vagrantcmdloc = VagrantBinaryUtility.getVagrantLocation(s);
		File wd = CDKServerUtility.getWorkingDirectory(s);
		Process p = callProcess(vagrantcmdloc, ArgsUtil.parse(args), wd, env, true);
		return p;
	}

	public Process callMinishiftInteractive(IServer s, String args, String launchConfigName, boolean skipCredentials)
			throws CoreException, IOException {
		return callInteractive(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
	}

	public Process callMinishiftConsole(IServer s, String args, String launchConfigName, boolean skipCredentials)
			throws CoreException, IOException {
		Map<String, String> env = getEnvironment(s, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
		String minishift = MinishiftBinaryUtility.getMinishiftLocation(s);
		File wd = CDKServerUtility.getWorkingDirectory(s);
		Process p = callProcess(minishift, ArgsUtil.parse(args), wd, env, false);
		return p;
	}

	public static Process callProcess(String rootCommand, String[] args, File vagrantDir, Map<String, String> env,
			boolean interactive) throws IOException {
		ensureCommandOnPath(rootCommand, env);
		String[] envp = (env == null ? null : convertEnvironment(env));

		List<String> cmd = new ArrayList<>();
		cmd.add(rootCommand);
		cmd.addAll(Arrays.asList(args));
		Process p = null;
		if (interactive) {
			p = ProcessFactory.getFactory().exec(cmd.toArray(new String[0]), envp, vagrantDir,
					new PTY(PTY.Mode.TERMINAL));
		} else {
			p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), envp, vagrantDir);
		}
		return p;
	}

	public static String[] call(String rootCommand, String[] args, File vagrantDir, Map<String, String> env,
			int timeout, boolean interactive) throws IOException, CommandTimeoutException {
		final Process p = callProcess(rootCommand, args, vagrantDir, env, interactive);

		InputStream errStream = p.getErrorStream();
		InputStream inStream = p.getInputStream();

		StreamGobbler inGob = new StreamGobbler(inStream);
		StreamGobbler errGob = new StreamGobbler(errStream);

		inGob.start();
		errGob.start();

		Integer exitCode = null;
		if (p.isAlive()) {

			exitCode = ThreadUtils.runWithTimeout(timeout, new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					return p.waitFor();
				}
			});
		} else {
			exitCode = p.exitValue();
		}

		List<String> inLines = null;
		if (exitCode == null) {
			inGob.cancel();
			errGob.cancel();

			// Timeout reached
			p.destroyForcibly();
			inLines = inGob.getOutput();
			List<String> errLines = errGob.getOutput();
			throw new CommandTimeoutException(inLines, errLines);
		} else {
			inLines = inGob.getOutput();
		}

		return (String[]) inLines.toArray(new String[inLines.size()]);
	}

	private static class StreamGobbler extends Thread {
		InputStream is;
		ArrayList<String> ret = new ArrayList<String>();
		private boolean canceled = false;
		private boolean complete = false;

		public StreamGobbler(InputStream is) {
			this.is = is;
		}

		private synchronized void add(String line) {
			ret.add(line);
		}

		private synchronized ArrayList<String> getList() {
			return ret;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while (!isCanceled() && (line = br.readLine()) != null)
					add(line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			if (is != null) {
				try {
					is.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
			setComplete();
		}

		private synchronized void setComplete() {
			complete = true;
		}

		private synchronized boolean isComplete() {
			return complete;
		}

		private synchronized void setCanceled() {
			canceled = true;
		}

		private synchronized boolean isCanceled() {
			return canceled;
		}

		public void cancel() {
			setCanceled();
			if (is != null) {
				try {
					is.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}

		private void waitComplete(long delay, long maxwait) {
			long start = System.currentTimeMillis();
			long end = start + maxwait;
			while (!isComplete() && System.currentTimeMillis() < end) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ie) {

				}
			}
			if (!isComplete()) {
				cancel();
			}
		}

		private static final long MAX_WAIT_AFTER_TERMINATION = 5000;
		private static final long DELAY = 100;

		/**
		 * Wait a maximum 5 seconds for the streams to finish reading whatever is in the
		 * pipeline
		 * 
		 * @return
		 */
		public List<String> getOutput() {
			waitComplete(DELAY, MAX_WAIT_AFTER_TERMINATION);
			return getList();
		}
	}
}
