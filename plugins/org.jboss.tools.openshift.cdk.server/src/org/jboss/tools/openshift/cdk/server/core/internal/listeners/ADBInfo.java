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
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.VagrantLaunchUtility;

public class ADBInfo {
	
	public static final String SHARED_INFO_KEY = "cdk.sharedinfo.adbInfo";
	
	int openshiftPort = 8443;
	String openshiftHost = "https://10.1.2.2";
	
	HashMap<String,String> env;
	public ADBInfo(HashMap<String,String> env) throws URISyntaxException {
		this.env = env;
		String dockerHost = env.get("DOCKER_HOST");
		if( dockerHost != null ) {
			URI url = new URI(dockerHost);
			String h = url.getHost();
			openshiftHost = "https://" + h;
		}
	}
	
	
	/**
	 * Run the remote vagrant command to create an adbinfo from a server
	 * @param server
	 * @return
	 */
	
	public static ADBInfo loadADBInfo(IServer server) {
		
		String[] args = new String[]{CDKConstants.VAGRANT_CMD_ADBINFO, CDKConstants.VAGRANT_FLAG_NO_COLOR};
		HashMap<String,String> env = new HashMap<String,String>(System.getenv());
		
    	String vagrantcmdloc = CDKConstantUtility.getVagrantLocation(server);
		
    	CDKServer cdkServer = (CDKServer)server.loadAdapter(CDKServer.class, new NullProgressMonitor());
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
		if( passCredentials ) {
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
			env.put(userKey, cdkServer.getUsername());
			env.put(passKey, cdkServer.getPassword());
		}
		
		HashMap<String,String> adbEnv = new HashMap<String,String>();
	    try {
	    	String[] lines = VagrantLaunchUtility.call(vagrantcmdloc, args,  CDKServerUtility.getWorkingDirectory(server), env);
			String setEnvVarCommand = Platform.getOS().equals(Platform.OS_WIN32) ? "setx " : "export ";
			String setEnvVarDelim = Platform.getOS().equals(Platform.OS_WIN32) ? " " : "=";
			for(String oneAppend : lines) {
				String[] allAppends = oneAppend.split("\n");
				for( int i = 0; i < allAppends.length; i++ ) {
					if( allAppends[i].trim().startsWith(setEnvVarCommand)) {
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
			return new ADBInfo(adbEnv);
		} catch( URISyntaxException urise) {
			CDKCoreActivator.pluginLog().logError("Environment variable DOCKER_HOST is not a valid uri:  " + env.get("DOCKER_HOST"), urise);
		} catch(IOException | TimeoutException ce) {
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to vagrant adbinfo. ", ce);
		}
		return null;
	}
	
}