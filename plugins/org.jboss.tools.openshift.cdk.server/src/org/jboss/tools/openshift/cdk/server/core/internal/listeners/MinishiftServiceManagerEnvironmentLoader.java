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
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;

public class MinishiftServiceManagerEnvironmentLoader extends ServiceManagerEnvironmentLoader {

	public MinishiftServiceManagerEnvironmentLoader() {
		super(TYPE_MINISHIFT);
	}

	public ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server) {
		// Load the docker env
		Map<String, String> adbEnv = loadDockerEnv(server);

		// Load the minishift console --machine-readable
		Properties props = loadOpenshiftConsoleDetails(server);

		// merge the two
		Map<String, String> merged = merge(adbEnv, props);
		
		String dotMinishift = System.getProperty("user.home") + File.separator + ".minishift";
		Properties dotCDK = CDKServerUtility.getDotCDK(dotMinishift);
		merged = merge(merged, dotCDK);
		
		try {
			return new ServiceManagerEnvironment(merged);
		} catch (URISyntaxException urise) {
			CDKCoreActivator.pluginLog()
					.logError("Environment variable DOCKER_HOST is not a valid uri:  " + merged.get("DOCKER_HOST"), urise);
			return null;
		}
	}


	protected Map<String, String> loadDockerEnv(IServer server) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server);
		String cmdLoc = MinishiftBinaryUtility.getMinishiftLocation(server);
		String[] args = new String[] { "docker-env" };
		File wd = JBossServerCorePlugin.getServerStateLocation(server).toFile();
		try {
			HashMap<String, String> adbEnv = callAndParseEnvVar(env, args, cmdLoc, wd);
			return adbEnv;
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError("Unable to successfully complete a call to minishift docker-env ",
					ioe);
		}
		return new HashMap<String, String>();
	}

	protected Properties loadOpenshiftConsoleDetails(IServer server) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server);
		String[] args = new String[] { "console", "--machine-readable" };
		File wd = JBossServerCorePlugin.getServerStateLocation(server).toFile();
		String cmdLoc = MinishiftBinaryUtility.getMinishiftLocation(server);
		try {
			Properties ret = callAndParseProperties(env, args, cmdLoc, wd);
			return ret;
		} catch (IOException ce) {
			CDKCoreActivator.pluginLog()
					.logError("Unable to successfully complete a call to minishift console --machine-readable. ", ce);
		}
		return new Properties();
	}

}
