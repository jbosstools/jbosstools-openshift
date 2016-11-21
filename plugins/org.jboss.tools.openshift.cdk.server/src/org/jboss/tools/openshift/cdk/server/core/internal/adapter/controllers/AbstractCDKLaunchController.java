/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.VagrantBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.OpenShiftNotReadyPollingException;

public abstract class AbstractCDKLaunchController extends AbstractSubsystemController 
	implements ILaunchServerController, IExternalLaunchConstants {
	
	
	public static final String FLAG_INITIALIZED = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.launch.isInitialized";
	
	@Override
	public IStatus canStart(String launchMode) {
		return Status.OK_STATUS;
	}

	@Override
	public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
			throws CoreException {
		if( !isInitialized(workingCopy)) {
			initialize(workingCopy);
		}
		performOverrides(workingCopy);
	}

	protected boolean isInitialized(ILaunchConfigurationWorkingCopy wc) throws CoreException{
		return wc.hasAttribute(FLAG_INITIALIZED) && wc.getAttribute(FLAG_INITIALIZED, (Boolean)false);
	}

	protected abstract void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException;

	
	protected abstract void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException;

	@Override
	public abstract void launch(ILaunchConfiguration configuration, String mode, 
			ILaunch launch, IProgressMonitor monitor) throws CoreException;

	
	protected IProcess addProcessToLaunch(Process p, ILaunch launch, IServer s) {
		Map<String, String> processAttributes = new HashMap<String, String>();
		String vagrantcmdloc = VagrantBinaryUtility.getVagrantLocation(s);
		String progName = new Path(vagrantcmdloc).lastSegment();
		launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false");
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, progName);
		IProcess process = new RuntimeProcess(launch, p, vagrantcmdloc, processAttributes) {
			protected IStreamsProxy createStreamsProxy() {
				return null;
			}
		};
		launch.addProcess(process);
		return process;
	}
	
	protected void linkTerminal(Process p) {
		InputStream in = p.getInputStream();
		InputStream err = p.getErrorStream();
		OutputStream out = p.getOutputStream();
		Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "org.eclipse.tm.terminal.connector.streams.launcher.streams");
		properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, "org.eclipse.tm.terminal.connector.streams.StreamsConnector");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, getServer().getName());
		properties.put(ITerminalsConnectorConstants.PROP_LOCAL_ECHO, false);
		properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDIN, out);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDOUT, in);
		properties.put(ITerminalsConnectorConstants.PROP_STREAMS_STDERR, err);
		ITerminalService service = TerminalServiceFactory.getService();
		service.openConsole(properties, null);
	}
	
	protected LaunchManager getLaunchManager() {
		return (LaunchManager)DebugPlugin.getDefault().getLaunchManager();
	}
	 
	protected IDebugEventSetListener getDebugListener(final IProcess[] processes, final ILaunch launch) {
		return new IDebugEventSetListener() { 
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				if (events != null) {
					int size = events.length;
					for (int i = 0; i < size; i++) {
						if (processes[0] != null && processes[0].equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
							// Register this launch as terminated
							((LaunchManager)getLaunchManager()).fireUpdate(new ILaunch[] {launch}, LaunchManager.TERMINATE);
							processTerminated(getServer(), processes[0], this);
							DebugPlugin.getDefault().removeDebugEventListener(this);
						}
					}
				}
			}
		};
	}
	
	private void processTerminated(IServer server, IProcess p, IDebugEventSetListener listener) {
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(server);
		new Thread() {
			@Override
			public void run() {
				handleProcessTerminated(p, beh);
			}
		}.start();
		
		if( listener != null ) {
			DebugPlugin.getDefault().removeDebugEventListener(listener);
		}
	}
	
	

	/*
	 * An attempt to start when the CDK is already started
	 * will return a non-zero exit status (ie fail)
	 */
	protected static final boolean MULTIPLE_START_FAIL = false;
	
	/*
	 * An attempt to start when the CDK is already started
	 * will return a 0 exit status (ie success)
	 */
	protected static final boolean MULTIPLE_START_SUCCESS = false;
	
	protected boolean getMultipleStartBehavior() {
		return MULTIPLE_START_SUCCESS;
	}
	
	protected void handleProcessTerminated(IProcess p, ControllableServerBehavior beh) {
		if( getMultipleStartBehavior() == MULTIPLE_START_SUCCESS) {
			boolean handled = handleStartCommandExitCodeFailure(p, beh);
			if( handled ) 
				return;
		}
		
		processTerminatedDelay();
		
		// Poll the server once more 
		AbstractCDKPoller vp = getCDKPoller();
		IStatus stat = vp.getCurrentStateSynchronous(getServer());
		if( stat.isOK()) {
			beh.setServerStarted();
			beh.setRunMode("run");
		} else {
			// The vm is now in a confused state.  
			if( vp.getPollingException() instanceof OpenShiftNotReadyPollingException) {
				// The vm is running but openshift isn't available.  
				handleOpenShiftUnavailable(beh, (OpenShiftNotReadyPollingException)vp.getPollingException());
			} else {
				beh.setServerStopped();
			}
		}
	}
	
	/*
	 * Handle the exit code scenario when we know a non-zero exit code definitely means
	 * the server failed to start, and cannot mean it is already started
	 * 
	 * return true if handled, false if more handling required
	 */
	protected boolean handleStartCommandExitCodeFailure(IProcess p, ControllableServerBehavior beh) {
		try {
			int exit = p.getExitValue();
			if( exit != 0 ) {
				handleStartupCommandFailed(beh);
				return true;
			}
		} catch (DebugException e) {
			// Should never happen, process should be terminated already wtf
			CDKCoreActivator.pluginLog().logError(e);
			try {
				p.terminate();
			} catch(DebugException de) {
				CDKCoreActivator.pluginLog().logError(de);
			}
			handleStartupCommandFailed(beh);
			return true;
		}
		return false;
	}

	protected abstract AbstractCDKPoller getCDKPoller();
	
	protected void processTerminatedDelay() {
		// Do nothing, subclass may sleep here depending on their use case
	}
	

	protected void handleStartupCommandFailed(ControllableServerBehavior beh) {
		IStatus s = CDKCoreActivator.statusFactory().errorStatus("The command to launch the CDK has failed. Please inspect the terminal for more information.");
		CDKCoreActivator.pluginLog().logStatus(s);
		beh.setServerStopped();
	}
	
	private void handleOpenShiftUnavailable(final IControllableServerBehavior beh, final OpenShiftNotReadyPollingException osnrpe) {
		// Log error?  Show dialog?  
		((ControllableServerBehavior)beh).setServerStarted();
		((Server)beh.getServer()).setMode("run");
		new Job(osnrpe.getMessage()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return CDKCoreActivator.statusFactory().errorStatus("Error contacting OpenShift", osnrpe);
			}
			
		}.schedule();
	}
	
	protected String getStartupLaunchName(IServer s) {
		return "Start " + s.getName();
	}
	
}
