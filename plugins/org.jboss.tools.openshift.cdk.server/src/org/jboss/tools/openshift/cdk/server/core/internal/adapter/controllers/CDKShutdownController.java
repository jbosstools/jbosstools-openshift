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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.control.impl.ITerminalControlForText;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.ide.eclipse.as.core.server.internal.PollThread;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.core.util.PollThreadUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IServerShutdownController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.as.core.server.controllable.IDeployableServerBehaviorProperties;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.jboss.tools.openshift.cdk.server.ui.internal.util.TerminalUtility;

public class CDKShutdownController extends AbstractSubsystemController implements IServerShutdownController, IExternalLaunchConstants {
	@Override
	public IStatus canStop() {
		return Status.OK_STATUS;
	}
	
	public ControllableServerBehavior getBehavior() {
		return (ControllableServerBehavior)getServer().loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
	}
	
	
	private void cancelPoller() {
		// Server is in process of starting or stopping... 
		Object pt = getBehavior().getSharedData(IDeployableServerBehaviorProperties.POLL_THREAD);
		if( pt instanceof PollThread ) {
			((PollThread) pt).cancel();
		}
	}
	

	protected boolean getRequiresForce() {
		Object o = getControllableBehavior().getSharedData(AbstractStartJavaServerLaunchDelegate.NEXT_STOP_REQUIRES_FORCE);
		return o == null ? false : ((Boolean)o).booleanValue();
	}
	
	protected void setNextStopRequiresForce(boolean val) {
		getControllableBehavior().putSharedData(AbstractStartJavaServerLaunchDelegate.NEXT_STOP_REQUIRES_FORCE, val);
	}
	
	@Override
	public void stop(boolean force) {
		getBehavior().setServerStopping();

		IStatus state = PollThreadUtils.isServerStarted(getServer(), new VagrantPoller());
		boolean started = state.isOK();
		boolean requiresCancelation = true;  // Bug in poller, assume always need to cancel existing startup
		if( !started ) {
			if( state.getSeverity() == IStatus.ERROR ) {
				// server is stopped, cancel the poller
				cancelPoller();
				getBehavior().setServerStopped();
				return;
			}
			// server is either starting or stopping, so cancel the transition
			requiresCancelation = true;
		}
		
		try {
			shutdownViaExternalTools();
		} catch(CoreException ce) {
			CDKCoreActivator.getDefault().getLog().log(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Error shutting down server", ce));
			getBehavior().setServerStarted();
		}
	}
	
	private void shutdownViaExternalTools() throws CoreException {
		String cmd = CDKConstants.VAGRANT_CMD_HALT + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR; 
		
		ILaunchConfigurationWorkingCopy wc = new VagrantLaunchUtility().createExternalToolsLaunchConfig(getServer(), 
				cmd, "Shutdown " + getServer().getName());
		ControllableServerBehavior beh = (ControllableServerBehavior)getServer().loadAdapter(
					ControllableServerBehavior.class, new NullProgressMonitor());
		try {
			ILaunch launch2 = wc.launch("run", new NullProgressMonitor());
			final IProcess[] processes = launch2.getProcesses();
			beh.setServerStopping();
			
			if( processes != null && processes.length >= 1 && processes[0] != null ) {
				DebugPlugin.getDefault().addDebugEventListener(getDebugListener(processes));
			}
		} catch(CoreException ce) {
			CDKCoreActivator.getDefault().getLog().log(ce.getStatus());
		}
	}
	
	private IDebugEventSetListener getDebugListener(final IProcess[] processes) {
		return new IDebugEventSetListener() { 
			public void handleDebugEvents(DebugEvent[] events) {
				if (events != null) {
					int size = events.length;
					for (int i = 0; i < size; i++) {
						if (processes[0] != null && processes[0].equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
							processTerminated(getServer(), processes[0], this);
						}
					}
				}
			}
		};
	}
	
	private void processTerminated(IServer server,IProcess process, IDebugEventSetListener listener) {
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(server);
		new Thread() {
			public void run() {
				try {
					// sleep to allow vagrant to unlock queries. 
					Thread.sleep(1000);
				} catch( InterruptedException ie) {}
				
				// Poll the server once more 
				IStatus stat = new VagrantPoller().getCurrentStateSynchronous(getServer());
				if( stat.getSeverity() == IStatus.ERROR) {
					beh.setServerStopped();
					beh.setRunMode("run");
				} else {
					// The shutdown failed. We'll set the server to started and indicate a requiresForce
					beh.setServerStarted();
					setNextStopRequiresForce(true);
				}	
			}
		}.start();
		DebugPlugin.getDefault().removeDebugEventListener(listener);
	}
	
	
	private ILaunchConfigurationWorkingCopy findLaunchConfig(IServer s) throws CoreException {
		return ExternalLaunchUtil.findExternalToolsLaunchConfig(s, getStartupLaunchName(s));
	}
	
	private String getStartupLaunchName(IServer s) {
		return "Shutdown " + s.getName();
	}
	
	
	private void shutdownViaTerminal(boolean requiresCancelation) {
		// Via terminal is as forceful as we can get
		if(getRequiresForce()) {
			setNextStopRequiresForce(false);
		}
		
		final ITerminalControlForText[] control = new ITerminalControlForText[1];
		control[0] = null;
		final Map<String, Object> props = TerminalUtility.getPropertiesForServer(getServer());
		Display.getDefault().syncExec(new Runnable(){
			public void run() {
				control[0] = TerminalUtility.findTerminalControl(props);
			}
		});
		
		final CustomDone cd = new CustomDone();
		if( control[0] == null ) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					TerminalUtility.openConsole(props, cd);
				}
			});
			// Wait for done
			while(cd.getStatus() == null ) {
				try {
					Thread.sleep(200);
				} catch(InterruptedException ie) {
					// TODO
				}
			}
			Display.getDefault().syncExec(new Runnable(){
				public void run() {
					control[0] = TerminalUtility.findTerminalControl(props);
				}
			});
		}
		
		String command2 = "vagrant halt";
		final CDKServer cdkServer = (CDKServer)getServer().loadAdapter(CDKServer.class, new NullProgressMonitor());
		// TODO UNDO THESE CHANGES
    	boolean passCredentials = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false);
		if( passCredentials ) {
			String userKey = cdkServer.getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME);
			String passKey = cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK_ENV_SUB_PASSWORD);
			String user = cdkServer.getUsername();
			String pass = cdkServer.getPassword();
			command2 = userKey + "=" + user + " " + passKey + "=" + pass + " " + command2;
		}
		final String command = "\n" + command2 + "\n";

		
		if( control[0] != null ) {
			OutputStream os = control[0].getOutputStream();
			try {
				if( requiresCancelation ) {
					os.write(new byte[]{3}); // Hitting ctrl+c in case the startup has stalled
					try {
						Thread.sleep(300); // delay since it requires 2 ctrl+c to cancel properly
					} catch(InterruptedException ie) {}
					os.write(new byte[]{3}); // Hitting ctrl+c in case the startup has stalled
				}
				os.write(command.getBytes());
				os.flush();
				try {
					Thread.sleep(1500);
				} catch(InterruptedException ie) {
					// ignore
				}
				launchShutdownPoller();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		} else {
			getBehavior().setServerStarted();
		}
	}
	
	private void launchShutdownPoller() {
		PollThreadUtils.pollServer(getServer(), IServerStatePoller2.SERVER_DOWN, new VagrantPoller());
	}
	
	private class CustomDone implements ITerminalService.Done {
		private IStatus stat = null;
		public void done(IStatus status) {
			this.stat = status;
		}
		public IStatus getStatus() {
			return stat;
		}
	}
}
