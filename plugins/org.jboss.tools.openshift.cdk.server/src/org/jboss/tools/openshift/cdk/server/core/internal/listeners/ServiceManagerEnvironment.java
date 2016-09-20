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
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.VagrantLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.VagrantTimeoutException;

public class ServiceManagerEnvironment {
	
	public static final String SHARED_INFO_KEY = "cdk.sharedinfo.serviceManagerEnvironment";
	
	int openshiftPort = 8443;
	String openshiftHost = "https://10.1.2.2";
	
	Map<String,String> env;
	public ServiceManagerEnvironment(Map<String,String> env) throws URISyntaxException {
		this.env = env;
		String dockerHost = env.get("DOCKER_HOST");
		if( dockerHost != null ) {
			URI url = new URI(dockerHost);
			String h = url.getHost();
			openshiftHost = "https://" + h;
		}
	}
	
	public String getOpenShiftHost() {
		return openshiftHost;
	}
	
	public int getOpenShiftPort() {
		return openshiftPort;
	}
	
	public String get(String key) {
		return env == null ? null : env.get(key);
	}
	
	public static ServiceManagerEnvironment getOrLoadServiceManagerEnvironment(IServer server, boolean save) {
		return getOrLoadServiceManagerEnvironment(server, save, 1);
	}
	
	public static ServiceManagerEnvironment getOrLoadServiceManagerEnvironment(IServer server, boolean save, int maxTries) {
		IControllableServerBehavior behavior = JBossServerBehaviorUtils.getControllableBehavior(server);
		Object o = behavior.getSharedData(SHARED_INFO_KEY);
		ServiceManagerEnvironment ret = null;
		if( !(o instanceof ServiceManagerEnvironment )) {
			ret = loadServiceManagerEnvironment(server, maxTries);
		} else {
			ret = (ServiceManagerEnvironment)o;
		}
		if( save ) {
			behavior.putSharedData(SHARED_INFO_KEY, ret);
		}
		return ret;
	}
	
	public static void clearServiceManagerEnvironment(IServer server) {
		IControllableServerBehavior behavior = JBossServerBehaviorUtils.getControllableBehavior(server);
		behavior.putSharedData(SHARED_INFO_KEY, null);
	}
	
	
	public static ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server, int maxTries) {
		for( int i = 0; i < maxTries; i++ ) {
			ServiceManagerEnvironment env = loadServiceManagerEnvironment(server);
			if( env != null )
				return env;
		}
		
		return null;
	}
	
	/**
	 * Run the remote vagrant command to create an service-manager from a server
	 * @param server
	 * @return
	 */
	public static ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server) {
		
		String[] args = new String[]{
				CDKConstants.VAGRANT_CMD_SERVICE_MANAGER, 
				CDKConstants.VAGRANT_CMD_SERVICE_MANAGER_ARG_ENV};

    	Map<String,String> env = CDKLaunchEnvironmentUtil.createEnvironment(server);
    	// Docs indicate any value here is fine, so no need to check for existing property
		env.put("VAGRANT_NO_COLOR", "1");
    	String vagrantcmdloc = CDKConstantUtility.getVagrantLocation(server);
	    try {
	    	String[] lines = VagrantLaunchUtility.callMachineReadable(vagrantcmdloc, args,  CDKServerUtility.getWorkingDirectory(server), env);
	    	return parseLines(lines);
		} catch( URISyntaxException urise) {
			CDKCoreActivator.pluginLog().logError("Environment variable DOCKER_HOST is not a valid uri:  " + env.get("DOCKER_HOST"), urise);
		} catch(IOException ce) {
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant service-manager. ", ce);
		} catch(VagrantTimeoutException ce) {
			// Try to salvage it, it could be the process never terminated but it got all the output
			List<String> inLines = ce.getInLines();
			if( inLines != null ) {
				String[] asArr = (String[]) inLines.toArray(new String[inLines.size()]);
				try {
					ServiceManagerEnvironment ret = parseLines(asArr);
					if( ret != null ) {
						return ret;
					}
				} catch(URISyntaxException urise) {
					CDKCoreActivator.pluginLog().logError("Environment variable DOCKER_HOST is not a valid uri:  " + env.get("DOCKER_HOST"), urise);
				}
			}
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant service-manager. ", ce);
		}
		return null;
	}
	
	public static ServiceManagerEnvironment parseLines(String[] lines) throws URISyntaxException {
		HashMap<String,String> adbEnv = new HashMap<>();
		
    	String setEnvWin = "setx ";
    	String setEnvNix = "export ";
		
		for(String oneAppend : lines) {
			String[] allAppends = oneAppend.split("\n");
			for( int i = 0; i < allAppends.length; i++ ) {
				String setEnvVarCommand = null;
				String setEnvVarDelim = null;
				if( allAppends[i].trim().startsWith(setEnvWin)) {
					setEnvVarCommand = setEnvWin;
					setEnvVarDelim = " ";
				} else if( allAppends[i].trim().startsWith(setEnvNix)) {
					setEnvVarCommand = setEnvNix;
					setEnvVarDelim = "=";
				}
				if( setEnvVarCommand != null ) {
					String lineRemainder = allAppends[i].trim().substring(setEnvVarCommand.length());
					int eq = lineRemainder.indexOf(setEnvVarDelim);
					if( eq != -1 ) {
						String k = lineRemainder.substring(0, eq);
						String v = lineRemainder.substring(eq+1);
						adbEnv.put(k, v);
					}
				}
			}
		}
		
		if( adbEnv.size() > 0 ) {
			return new ServiceManagerEnvironment(adbEnv);
		}
		return null;
	}
}