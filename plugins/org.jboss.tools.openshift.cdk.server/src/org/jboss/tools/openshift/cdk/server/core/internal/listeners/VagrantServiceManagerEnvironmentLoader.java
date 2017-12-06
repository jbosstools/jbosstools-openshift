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
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;

public class VagrantServiceManagerEnvironmentLoader extends ServiceManagerEnvironmentLoader {
	private static final String DEFAULT_IMAGE_REGISTRY_URL = "https://hub.openshift.rhel-cdk.10.1.2.2.xip.io";

	public VagrantServiceManagerEnvironmentLoader() {
		super(TYPE_VAGRANT);
	}

	public ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server) {
		return loadServiceManagerEnvironment(server, false);
	}

	public ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server, boolean suppressErrors) {
		// loading service-manager env
		Map<String, String> adbEnv = loadDockerEnv(server);

		if (adbEnv != null) {
			// read the .cdk file
			Properties dotcdkProps = CDKServerUtility.getDotCDK(server);

			// merge the two
			Map<String, String> merged = merge(adbEnv, dotcdkProps);

			try {
				if (merged != null) {
					// Manually set default for image registry url
					merged.put(ServiceManagerEnvironment.KEY_DEFAULT_IMAGE_REGISTRY, DEFAULT_IMAGE_REGISTRY_URL);

					return new ServiceManagerEnvironment(merged);
				}
			} catch (URISyntaxException urise) {
				if (!suppressErrors) {
					String err = "Environment variable DOCKER_HOST is not a valid uri:  "
							+ merged.get(ServiceManagerEnvironment.KEY_DOCKER_HOST);
					CDKCoreActivator.pluginLog().logError(err, urise);
				}
			}
		}
		return null;
	}

	protected Map<String, String> loadDockerEnv(IServer server) {
		Map<String, String> env = getEnv(server);
		String[] args = new String[] { CDKConstants.VAGRANT_CMD_SERVICE_MANAGER,
				CDKConstants.VAGRANT_CMD_SERVICE_MANAGER_ARG_ENV };
		args = CDK32Server.getArgsWithProfile(server, args);

		String cmdLoc = VagrantBinaryUtility.getVagrantLocation(server);
		File wd = CDKServerUtility.getWorkingDirectory(server);
		try {
			HashMap<String, String> adbEnv = callAndParseEnvVar(env, args, cmdLoc, wd);
			return adbEnv;
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog()
					.logError("Unable to successfully complete a call to \"vagrant service-manager env\"", ioe);
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
