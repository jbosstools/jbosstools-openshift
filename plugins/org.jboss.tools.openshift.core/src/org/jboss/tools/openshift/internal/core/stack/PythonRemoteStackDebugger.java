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
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.jboss.tools.openshift.core.stack.RemoteStackDebugger;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.google.gson.stream.JsonWriter;

/**
 * @author Red Hat Developers
 *
 */
public class PythonRemoteStackDebugger implements RemoteStackDebugger, IDebugProtocolClient {

	private static final String ID_REMOTE_DSP_APPLICATION = "org.eclipse.lsp4e.debug.launchType";

	@Override
	public boolean isValid(String stackType, String stackVersion) {
		return stackType.contains("python");
	}

	@Override
	public void startRemoteDebugger(IProject project, String stackType, String stackVersion, int port, Map<String, String> env, IProgressMonitor monitor) throws CoreException {
		try {
		  waitForDebugger(port);
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
	
  private void checkDebugger(int port) throws IOException {
    try (Socket socket = new Socket("localhost", port)) {
      Launcher<IDebugProtocolServer> launcher = DSPLauncher.createClientLauncher(this, socket.getInputStream(),
          socket.getOutputStream());
      launcher.startListening();
      InitializeRequestArguments arguments = new InitializeRequestArguments();
      arguments.setClientID("lsp4e.debug");
      String adapterId = "adapterId";
      arguments.setAdapterID(adapterId);
      arguments.setPathFormat("path");
      arguments.setSupportsVariableType(true);
      arguments.setSupportsVariablePaging(true);
      arguments.setLinesStartAt1(true);
      arguments.setColumnsStartAt1(true);
      arguments.setSupportsRunInTerminalRequest(true);

      try {
        launcher.getRemoteProxy().initialize(arguments).get(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new IOException(e);
      }
    }
  }
  
  /**
   * debugpy seems to have concurrent issues and if a request is sent just after
   * odo debug port-forward is running then initialize request is never processed
   * this the DSP launch configuration will block. This will manually sent DSP 
   * timed initialize request to make sure debugpy is remotely correctly setup
   * 
   * @param port the debugpy port
   * @throws IOException
   */
  private void waitForDebugger(int port) throws IOException {
    for(int i=0; i < 3;++i) {
      try {
        checkDebugger(port);
        return;
      } catch (IOException e) {}
    }
    throw new IOException("Failed to connect to remote Python debugger");
  }
}
