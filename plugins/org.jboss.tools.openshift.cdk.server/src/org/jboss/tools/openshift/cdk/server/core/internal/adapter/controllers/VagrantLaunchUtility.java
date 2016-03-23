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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

import static org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants.ATTR_ARGS;
import static org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants.ENVIRONMENT_VARS_KEY;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationLookupStrategy;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;

public class VagrantLaunchUtility {
	public ILaunchConfigurationWorkingCopy createExternalToolsLaunchConfig(IServer s, String args, String launchConfigName) throws CoreException {
		return setupLaunch(s, args, launchConfigName, s.getLaunchConfiguration(true, new NullProgressMonitor()));
	}
	
	public ILaunchConfigurationWorkingCopy setupLaunch(IServer s, String args, String launchConfigName, ILaunchConfiguration startupConfig) throws CoreException {
		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());
		String user = cdkServer.getUsername();
		String pass = null;
		try {
			pass = cdkServer.getPassword();
		} catch(UsernameChangedException uce) {
			pass = uce.getPassword();
			user = uce.getUser();
		}
		return setupLaunch(s, args, launchConfigName, startupConfig, user, pass);
	}
	
	public ILaunchConfigurationWorkingCopy setupLaunch(IServer s, String args, 
			String launchConfigName, ILaunchConfiguration startupConfig,
			String userName, String pass) throws CoreException {

		final CDKServer cdkServer = (CDKServer)s.loadAdapter(CDKServer.class, new NullProgressMonitor());

		ILaunchConfigurationWorkingCopy wc = findLaunchConfig(s, launchConfigName);
		
		wc.setAttributes(startupConfig.getAttributes());
		wc.setAttribute(ATTR_ARGS, args);
		
		
		// Set the environment flag
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
    	if( passCredentials) {
    		Map<String,String> existingEnvironment = startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String,String>)null);
    		if( existingEnvironment == null ) {
    			existingEnvironment = new HashMap<String,String>();
    		}
    		
    		if( userName == null ) {
    			// This is an error situation and the user should be made aware
    			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, 
    					"The credentials for " + s.getName() + " are invalid. No username found. Please open your server editor " 
    					+ "and set your access.redhat.com credentials."));
    		}
    		
    		HashMap<String,String> env = new HashMap<String,String>(existingEnvironment);
    		String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
    		env.put(userKey, userName);
    		
    		String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
    		if( pass == null ) {
    			// This is an error situation and the user should be made aware
    			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, 
    					"The credentials for " + s.getName() + " are invalid. No password found for username "
    					+ cdkServer.getUsername() + " for the access.redhat.com domain. Please open your server editor " + 
    							"set your access.redhat.com credentials."));
    		}
    		if( passKey != null && pass != null )
    			env.put(passKey, pass);
    		
    		wc.setAttribute(ENVIRONMENT_VARS_KEY, env);
    	} else {
    		wc.setAttribute(ENVIRONMENT_VARS_KEY, startupConfig.getAttribute(ENVIRONMENT_VARS_KEY, (Map<String,String>)null));
    	}
		return wc;
		
	}
	

	private ILaunchConfigurationWorkingCopy findLaunchConfig(IServer s, String launchName) throws CoreException {
		return ExternalLaunchUtil.findExternalToolsLaunchConfig(s, launchName);
	}
	
	/*
	 * The following methods are for dealing with raw calls to vagrant, 
	 * not using launch configurations at all. 
	 */

	/*
	 * Convert a string/string hashmap into an array of string environment
	 * variables as required by java.lang.Runtime This will super-impose the
	 * provided environment variables ON TOP OF the existing environment in
	 * eclipse, as users may not know *all* environment variables that need to
	 * be set, or to do so may be tedious.
	 */
	public static String[] convertEnvironment(Map<String, String> env) {
		if (env == null || env.size() == 0)
			return null;

		// Create a new map based on pre-existing environment of Eclipse
		Map<String, String> original = new HashMap<>(System.getenv());

		// Add new environment on top of existing
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

	public static String[] call(String rootCommand, String[] args, File vagrantDir, Map<String, String> env)
			throws IOException, TimeoutException {
		return call(rootCommand, args, vagrantDir, env, 30000);
	}
	
	private static void ensureCommandOnPath(String rootCommand, Map<String, String> env) {
		CommandLocationLookupStrategy.get().ensureOnPath(env, new Path(rootCommand).removeLastSegments(1).toOSString());
	}
	
	public static String[] call(String rootCommand, String[] args, File vagrantDir, Map<String, String> env,
			int timeout) throws IOException, TimeoutException {
		ensureCommandOnPath(rootCommand, env);
		String[] envp = (env == null ? null : convertEnvironment(env));

		List<String> cmd = new ArrayList<>();
		cmd.add(rootCommand);
		cmd.addAll(Arrays.asList(args));
		final Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), envp, vagrantDir);

		InputStream errStream = p.getErrorStream();
		InputStream inStream = p.getInputStream();

		Integer exitCode = ThreadUtils.runWithTimeout(timeout, new Callable<Integer>() {
			@Override
		 	public Integer call() throws Exception {
				return p.waitFor();
			}
		});
		
		List<String> inLines = null;
		if( exitCode == null ) {
			// Timeout reached
			p.destroyForcibly();
			inLines = readStream(inStream);
			List<String> errLines = readStream(errStream);
			throw new TimeoutException(getTimeoutError(inLines, errLines));
		} else {
			inLines = readStream(inStream);
		}
		
		return (String[]) inLines.toArray(new String[inLines.size()]);
	}
	
	private static List<String> readStream(InputStream inStream) {
		ArrayList<String> lines = new ArrayList<>();
		try (
			BufferedInputStream is = new BufferedInputStream(inStream);
			Scanner inScanner = new Scanner(is);
			) {
			while (inScanner.hasNextLine()) {
				lines.add(inScanner.nextLine());
			}
		} catch(IOException ioe) {
			// ignore autoclosed ioexception
		}
		return lines;
	}
	
	private static String getTimeoutError(List<String> output, List<String> err) {
		StringBuilder msg = new StringBuilder();
		msg.append("Process output:\n");
		output.forEach(line -> msg.append("   ").append(line));
		err.forEach(line -> msg.append("   ").append(line));
		return msg.toString();
	}
}
