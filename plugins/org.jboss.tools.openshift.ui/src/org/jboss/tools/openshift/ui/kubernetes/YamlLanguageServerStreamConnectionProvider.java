/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/

package org.jboss.tools.openshift.ui.kubernetes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.osgi.framework.Bundle;

public class YamlLanguageServerStreamConnectionProvider extends ProcessStreamConnectionProvider implements StreamConnectionProvider {
	
	private static final String YAML_LANGUAGE_SERVER_LOCATION = "yaml-language-server-0.0.1-SNAPSHOT";
	private static final String YAML_LANGUAGE_SERVER_SERVERJS_LOCATION = YAML_LANGUAGE_SERVER_LOCATION + "/src/server.js";
	private static final String NODE_PARAMETER_USE_STDIO = "--stdio";
	private static final String DEFAULT_MACOS_NODE_PATH = "/usr/local/bin/node";

	public YamlLanguageServerStreamConnectionProvider() throws CoreException {

		File nodeJsLocation = getNodeJsLocation();
		if (nodeJsLocation == null) {
			return;
		}

		File serverLocation = getServerLocation();
		if (serverLocation == null) {
			return;
		}

		setCommands(Arrays.asList(new String[] {
				nodeJsLocation.getAbsolutePath(),
				serverLocation.getAbsolutePath(),
				NODE_PARAMETER_USE_STDIO
		}));

		setWorkingDirectory(SystemUtils.getUserDir().getAbsolutePath());
	}

	@Override
	public void start() throws IOException {
		super.start();
	}
	
	@Override
	public void stop() {
		super.stop();
	}
	
	@Override
	protected ProcessBuilder createProcessBuilder() {
		return super.createProcessBuilder();
	}

	private static File getNodeJsLocation() throws CoreException {
		String location = launchNodeLocationProvider();
		if (Platform.OS_MACOSX.equals(Platform.getOS())) {
			location = DEFAULT_MACOS_NODE_PATH;
		}

		if (StringUtils.isEmpty(location) 
				|| !Paths.get(location).toFile().exists()) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID, 
					"`node` is missing in your PATH, Kubernetes Language server requires node.\n" +
					"Please install `node` and make it available in your PATH"));
		}
		return new File(location);
	}

	private static String launchNodeLocationProvider() {
		String location = null;
		try (BufferedReader reader = 
				new BufferedReader(new InputStreamReader(new NodeLocationProvider().execute().getInputStream()))) {
			location = reader.readLine();
		} catch (IOException e) {
			// ignore
		}
		return location;
	}

	private static File getServerLocation() throws CoreException {
		try {
			Bundle bundle = Platform.getBundle(OpenShiftUIActivator.PLUGIN_ID);
			return new File(FileLocator.getBundleFile(bundle), YAML_LANGUAGE_SERVER_SERVERJS_LOCATION);
		} catch (IOException e) {
			IStatus error = StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID, "Could not find the Kubernetes langauge server path.", e);
			throw new CoreException(error);
		}
	}
	
	private static final class NodeLocationProvider {
		private static final String[] WINDOWS_WHICH_COMMAND = new String[] {"cmd", "/c", "where node"};
		private static final String[] UNIX_WHICH_COMMAND = new String[] {"/bin/bash", "-c", "which node"};

		private static String[] getCommand() {
			String[] command = UNIX_WHICH_COMMAND;
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				command = WINDOWS_WHICH_COMMAND;
			}
			return command;
		}

		public Process execute() throws IOException {
			return Runtime.getRuntime().exec(getCommand());
		}
		
	}
}
