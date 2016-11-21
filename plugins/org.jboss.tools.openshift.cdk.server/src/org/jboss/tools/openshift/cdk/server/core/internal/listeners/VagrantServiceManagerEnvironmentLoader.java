/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.VagrantBinaryUtility;

public class VagrantServiceManagerEnvironmentLoader extends ServiceManagerEnvironmentLoader {

	public VagrantServiceManagerEnvironmentLoader() {
		super(TYPE_VAGRANT);
	}

	public ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server) {
		// loading service-manager env
		Map<String, String> adbEnv = loadDockerEnv(server);
		
		if( adbEnv != null ) {
			// read the .cdk file
			Properties dotcdkProps = CDKServerUtility.getDotCDK(server);
			
			// merge the two
			Map<String, String> merged = merge(adbEnv, dotcdkProps);
			try {
				if (merged != null) {
					return new ServiceManagerEnvironment(merged);
				}
			} catch (URISyntaxException urise) {
				CDKCoreActivator.pluginLog()
						.logError("Environment variable DOCKER_HOST is not a valid uri:  " +
								merged.get(ServiceManagerEnvironment.KEY_DOCKER_HOST), urise);
			}
		}
		return null;
	}

	protected Map<String, String> loadDockerEnv(IServer server) {
		Map<String, String> env = getEnv(server);
		String[] args = new String[] { CDKConstants.VAGRANT_CMD_SERVICE_MANAGER,
				CDKConstants.VAGRANT_CMD_SERVICE_MANAGER_ARG_ENV };
		String cmdLoc = VagrantBinaryUtility.getVagrantLocation(server);
		File wd =  CDKServerUtility.getWorkingDirectory(server);
		try {
			HashMap<String, String> adbEnv = callAndParseEnvVar(env, args, cmdLoc, wd);
			return adbEnv;
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to \"vagrant service-manager env\"",
					ioe);
		}
		return null;
	}
	
	protected Map<String, String> getEnv(IServer server) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server);
		// Docs indicate any value here is fine, so no need to check for
		// existing property
		env.put("VAGRANT_NO_COLOR", "1");
		return env;
	}

}
