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
import static org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants.ATTR_APPEND_ENVIRONMENT_VARIABLES;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.ArgsUtil;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.listeners.CDKServerUtility;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;

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
		
		if( skipCredentials ) 
			return startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String, String>) null);
		
		final CDKServer cdkServer = (CDKServer) s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		// Set the environment flag
		boolean passCredentials = cdkServer.passCredentials();
		if (passCredentials) {
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

	/*
	 * This method is for cdk entries where you are customizing the environment
	 * to include credentials. 
	 */
	public ILaunchConfigurationWorkingCopy createExternalToolsLaunch(IServer s, String args, String launchConfigName,
			ILaunchConfiguration startupConfig, String commandLoc, boolean skipCredentials) throws CoreException {
		Map<String, String> env = getEnvironment(s, startupConfig, skipCredentials);
		return createExternalToolsLaunch(s, args, launchConfigName, startupConfig, 
				commandLoc, env, true);
	}
	
	/*
	 * Entry suitable for all entrants
	 */
	public ILaunchConfigurationWorkingCopy createExternalToolsLaunch(IServer s, String args, String launchConfigName,
			ILaunchConfiguration startupConfig, String commandLoc, Map<String,String> env, 
			boolean ensureOnPath) throws CoreException {

		ILaunchConfigurationWorkingCopy wc = findLaunchConfig(s, launchConfigName);
		wc.setAttributes(startupConfig.getAttributes());
		wc.setAttribute(ATTR_ARGS, args);
		// Set the environment flag
		env = (env == null ? new HashMap<>() : env);
		wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
		wc.setAttribute(ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		if (commandLoc != null) {
			wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, commandLoc);
			if( ensureOnPath ) {
				String cmdFolder = new Path(commandLoc).removeLastSegments(1).toOSString();
				CommandLocationLookupStrategy.get().ensureOnPath(env, cmdFolder);
			}
		}

		return wc;
	}

	private ILaunchConfigurationWorkingCopy findLaunchConfig(IServer s, String launchName) throws CoreException {
		return ExternalLaunchUtil.findExternalToolsLaunchConfig(s, launchName);
	}

	public String[] callMachineReadable(String rootCommand, String[] args, File dir,
			Map<String, String> env) throws IOException, CommandTimeoutException {
		return ProcessLaunchUtility.call(rootCommand, args, dir, env, 90000, false);
	}

	public Process callInteractive(IServer s, String args, String launchConfigName, boolean skipCredentials) throws CoreException, IOException {
		return callInteractive(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
	}

	public Process callInteractive(IServer s, String args, 
			String launchConfigName, ILaunchConfiguration startupConfig, boolean skipCredentials)
			throws CoreException, IOException {
		Map<String, String> env = getEnvironment(s, startupConfig, skipCredentials);
		String vagrantcmdloc = BinaryUtility.VAGRANT_BINARY.getLocation(s);
		File wd = CDKServerUtility.getWorkingDirectory(s);
		Process p = ProcessLaunchUtility.callProcess(vagrantcmdloc, ArgsUtil.parse(args), wd, env, true);
		return p;
	}

	public Process callMinishiftInteractive(IServer s, String args, String launchConfigName, boolean skipCredentials)
			throws CoreException, IOException {
		return callInteractive(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
	}

	public Process callMinishiftConsole(IServer s, String args, String launchConfigName, boolean skipCredentials)
			throws CoreException, IOException {
		Map<String, String> env = getEnvironment(s, s.getLaunchConfiguration(true, new NullProgressMonitor()), skipCredentials);
		String minishift = BinaryUtility.MINISHIFT_BINARY.getLocation(s);
		File wd = CDKServerUtility.getWorkingDirectory(s);
		Process p = ProcessLaunchUtility.callProcess(minishift, ArgsUtil.parse(args), wd, env, false);
		return p;
	}

	
	public Process callCRCConsole(IServer s, String args) 
			throws CoreException, IOException {
		String crc = BinaryUtility.CRC_BINARY.getLocation(s);
		File wd = new File(crc).getParentFile();
		Process p = ProcessLaunchUtility.callProcess(crc, ArgsUtil.parse(args), 
				wd, envFromLaunchConfig(s), false);
		return p;
	}
	
	public Map<String,String> envFromLaunchConfig(IServer server) throws CoreException {
		// Get the environment users customized in the launch config
		ILaunchConfiguration startupConfig = server.getLaunchConfiguration(true, new NullProgressMonitor());
		Map<String, String> startupLaunchEnv = startupConfig.getAttribute(ENVIRONMENT_VARS_KEY,
				(Map<String, String>) null);
		if (startupLaunchEnv == null) {
			startupLaunchEnv = new HashMap<>();
		}
		return startupLaunchEnv;
	}
}
