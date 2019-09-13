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
package org.jboss.tools.openshift.internal.cdk.server.core.listeners;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK32Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK3Server;

public class MinishiftServiceManagerEnvironmentLoader extends ServiceManagerEnvironmentLoader {

	public MinishiftServiceManagerEnvironmentLoader() {
		super(TYPE_MINISHIFT);
	}

	@Override
	public ServiceManagerEnvironment loadServiceManagerEnvironment(IServer server, boolean suppressErrors) {
		String minishiftLoc = BinaryUtility.MINISHIFT_BINARY.getLocation(server);
		if (minishiftLoc == null || minishiftLoc.isEmpty() || !(new File(minishiftLoc).exists())) {
			if (!suppressErrors) {
				String msg = "Server " + server.getName() + " does not have a minishift location defined.";
				CDKCoreActivator.pluginLog().logError(msg);
			}
			return null;
		}

		// Load the docker env
		Map<String, String> adbEnv = loadDockerEnv(server);

		// Load the minishift console --machine-readable
		Properties props = loadOpenshiftConsoleDetails(server, suppressErrors);
		String registry = getOpenshiftRegistry(server, suppressErrors);
		if (registry != null)
			props.put(ServiceManagerEnvironment.IMAGE_REGISTRY_KEY, registry);

		// merge the two 
		Map<String, String> merged = merge(adbEnv, props);

		Properties dotCDK = getCDKProperties(server);
		merged = merge(merged, dotCDK);

		File ocLocation = findOCLocation(server);
		if (ocLocation != null) {
			merged.put(OC_LOCATION_KEY, ocLocation.getAbsolutePath());
		}

		try {
			ServiceManagerEnvironment env = new ServiceManagerEnvironment(merged);
			env.setDefaultUsername(ServiceManagerEnvironment.DEFAULT_MINISHIFT_USER);
			return env;
		} catch (URISyntaxException urise) {
			if (!suppressErrors) {
				String msg = "Environment variable DOCKER_HOST is not a valid uri:  " + merged.get("DOCKER_HOST");
				CDKCoreActivator.pluginLog().logError(msg, urise);
			}
			return null;
		}
	}

	private String getMinishiftHome(IServer server) {
		String minishiftHomeDefault = System.getProperty("user.home") + File.separator
				+ CDKConstants.CDK_RESOURCE_DOTMINISHIFT;
		String minishiftHome = server.getAttribute(CDK3Server.MINISHIFT_HOME, minishiftHomeDefault);
		return minishiftHome;
	}

	private String getMinishiftProfileHome(IServer server) {
		String profile = server.getAttribute(CDK32Server.PROFILE_ID, (String) null);
		String msHome = getMinishiftHome(server);
		if (StringUtils.isEmpty(profile) || profile.equals(CDK32Server.MINISHIFT_DEFAULT_PROFILE)) {
			return msHome;
		}
		return new Path(msHome).append(CDKConstants.CDK32_RESOURCE_PROFILES).append(profile).toOSString();
	}

	private Properties getCDKProperties(IServer server) {
		String profileHome = getMinishiftProfileHome(server);
		Properties dotCDK = CDKServerUtility.getDotCDK(profileHome, CDKConstants.CDK_RESOURCE_CDK);
		return dotCDK;
	}

	private File findOCLocation(IServer server) {
		String profileHome = getMinishiftProfileHome(server);
		File root = new File(profileHome);
		if (root.exists()) {
			File cache = new File(root, "cache");
			File oc = new File(cache, "oc");
			if (oc.exists()) {
				String[] names = oc.list();
				if (names != null && names.length > 0) {
					Arrays.sort(names);
					String latest = names[names.length - 1];
					File latestF = new File(oc, latest);
					String platformDep = (Platform.getOS().equals(Platform.OS_WIN32) ? "oc.exe" : "oc");
					File ocBin = new File(latestF, platformDep);
					if (ocBin.exists())
						return ocBin;
				}
			}
		}
		return null;
	}

	protected String getOpenshiftRegistry(IServer server, boolean suppressErrors) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server, true);
		String cmdLoc = BinaryUtility.MINISHIFT_BINARY.getLocation(server);
		String[] args = new String[] { "openshift", "registry" };
		args = CDK32Server.getArgsWithProfile(server, args);
		File wd = JBossServerCorePlugin.getServerStateLocation(server).toFile();

		try {
			String[] lines = callAndGetLines(env, args, cmdLoc, wd);
			String invalidMsg = null;
			if (lines != null && lines.length > 0 && lines[0] != null) {
				String l = lines[0];
				if (validateHostPort(l)) {
					return l;
				}
				invalidMsg = "Call to '" + cmdLoc + " openshift registry' returned an invalid url: " + l;
			} else {
				invalidMsg = "Call to '" + cmdLoc
						+ " openshift registry' was unable to locate an image registry for server " + server.getName();
			}
			if (invalidMsg != null && !suppressErrors)
				CDKCoreActivator.pluginLog().logWarning(invalidMsg);
		} catch (IOException ioe) {
			if (!suppressErrors) {
				String errMsg = "Unable to successfully complete a call to minishift openshift registry.";
				CDKCoreActivator.pluginLog().logError(errMsg, ioe);
			}
		}
		return null;
	}

	private boolean validateHostPort(String string) {
		// https://stackoverflow.com/questions/2345063/java-common-way-to-validate-and-convert-hostport-to-inetsocketaddress
		try {
			// WORKAROUND: add any scheme to make the resulting URI valid.
			URI uri = new URI("my://" + string); // may throw URISyntaxException
			if (uri.getHost() == null || uri.getPort() == -1) {
				return false;
			}
		} catch (URISyntaxException ex) {
			// validation failed
			return false;
		}

		// validation succeeded
		return true;
	}

	protected Map<String, String> loadDockerEnv(IServer server) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server, true);
		String cmdLoc = BinaryUtility.MINISHIFT_BINARY.getLocation(server);
		String[] args = new String[] { "docker-env" };
		args = CDK32Server.getArgsWithProfile(server, args);

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

	protected Properties loadOpenshiftConsoleDetails(IServer server, boolean suppressError) {
		Map<String, String> env = CDKLaunchEnvironmentUtil.createEnvironment(server, true);
		String[] args = new String[] { "console", "--machine-readable" };
		args = CDK32Server.getArgsWithProfile(server, args);
		File wd = JBossServerCorePlugin.getServerStateLocation(server).toFile();
		String cmdLoc = BinaryUtility.MINISHIFT_BINARY.getLocation(server);
		try {
			Properties ret = callAndParseProperties(env, args, cmdLoc, wd);
			return ret;
		} catch (IOException ce) {
			if (!suppressError) {
				CDKCoreActivator.pluginLog().logError(
						"Unable to successfully complete a call to minishift console --machine-readable. ", ce);
			}
		}
		return new Properties();
	}

}
