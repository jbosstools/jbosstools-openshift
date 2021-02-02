/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.stack;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.google.gson.stream.JsonWriter;

/**
 * @author Red Hat Developers
 *
 */
public class PythonRemoteStackDebugger implements RemoteStackDebugger {

	private static final String ID_REMOTE_DSP_APPLICATION = "org.eclipse.lsp4e.debug.launchType";

	@Override
	public boolean isValid(String stackType, String stackVersion) {
		return stackType.contains("python");
	}

	@Override
	public void startRemoteDebugger(IProject project, String stackType, String stackVersion, int port, Map<String, String> env, IProgressMonitor monitor) throws CoreException {
		try {
			String name = "OpenShift remote (Python) " + project.getName();
			ILaunchConfigurationType launchConfigurationType = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchConfigurationType(ID_REMOTE_DSP_APPLICATION);
			ILaunchConfigurationWorkingCopy launchConfiguration = launchConfigurationType.newInstance(null, name);
			launchConfiguration.setAttribute(DSPPlugin.ATTR_DSP_MODE, DSPPlugin.DSP_MODE_CONNECT);
			launchConfiguration.setAttribute(DSPPlugin.ATTR_DSP_SERVER_PORT, port); //$NON-NLS-1$
			launchConfiguration.setAttribute(DSPPlugin.ATTR_DSP_SERVER_HOST, "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
			launchConfiguration.setAttribute(DSPPlugin.ATTR_CUSTOM_LAUNCH_PARAMS, true);
			launchConfiguration.setAttribute(DSPPlugin.ATTR_DSP_PARAM, getAdditionJSONSettings(project, env));
			launchConfiguration.launch("debug", monitor);
		} catch (IOException e) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(e));
		}
	}
	
	private String getAdditionJSONSettings(IProject project, Map<String, String> env) throws IOException {
		try (Writer writer = new StringWriter(); JsonWriter jsonWriter = new JsonWriter(writer)) {
			jsonWriter.beginObject();
			jsonWriter.name("request").value("attach");
			jsonWriter.name("pathMappings");
			jsonWriter.beginArray();
			jsonWriter.beginObject();
			jsonWriter.name("localRoot").value(project.getLocation().toOSString());
			jsonWriter.name("remoteRoot").value(env.getOrDefault("PROJECTS_ROOT", "/opt/app-root/src"));
			jsonWriter.endObject();
			jsonWriter.endArray();
			jsonWriter.name("debugOptions");
			jsonWriter.beginArray();
      if (Platform.OS_WIN32.equals(Platform.getOS())) {
        jsonWriter.value("WindowsClient");
      } else {
        jsonWriter.value("UnixClient");
      }
			jsonWriter.endArray();
			jsonWriter.endObject().flush();
			return writer.toString();
		}
	}
}
