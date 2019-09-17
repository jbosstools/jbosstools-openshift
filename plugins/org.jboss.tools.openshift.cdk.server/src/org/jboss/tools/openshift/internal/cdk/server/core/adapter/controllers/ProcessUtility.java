package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.wst.server.core.IServer;

public class ProcessUtility {

	public static IProcess addProcessToLaunch(Process p, ILaunch launch, IServer s, 
			boolean terminal, String cmdLoc) {
		Map<String, String> processAttributes = new HashMap<String, String>();
		String progName = new Path(cmdLoc).lastSegment();
		if (terminal) {
			launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false");
		}
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, progName);
		IProcess process = createProcess(terminal, launch, p, cmdLoc, processAttributes);
		launch.addProcess(process);
		return process;
	}

	public static IProcess createProcess(boolean terminal, ILaunch launch,
			Process p, String cmd, Map<String, String> attr) {
		IProcess process = null;
		if (terminal) {
			process = new RuntimeProcess(launch, p, cmd, attr) {
				protected IStreamsProxy createStreamsProxy() {
					return null;
				}
			};
		} else {
			process = new RuntimeProcess(launch, p, cmd, attr);
		}
		return process;
	}

}
