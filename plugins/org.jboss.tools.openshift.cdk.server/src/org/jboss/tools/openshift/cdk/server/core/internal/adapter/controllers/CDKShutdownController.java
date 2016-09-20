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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.PollThread;
import org.jboss.ide.eclipse.as.core.util.JBossServerBehaviorUtils;
import org.jboss.ide.eclipse.as.core.util.PollThreadUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IServerShutdownController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.AbstractStartJavaServerLaunchDelegate;
import org.jboss.tools.as.core.server.controllable.IDeployableServerBehaviorProperties;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstantUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;

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
		if( !started ) {
			if( state.getSeverity() == IStatus.ERROR ) {
				// server is stopped, cancel the poller
				cancelPoller();
				getBehavior().setServerStopped();
				return;
			}
		}
		
		try {
			shutdownViaTerminal();
		} catch(CoreException ce) {
			CDKCoreActivator.getDefault().getLog().log(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Error shutting down server", ce));
			getBehavior().setServerStarted();
		}
	}
	
	private void shutdownViaTerminal() throws CoreException {
		String cmd = CDKConstants.VAGRANT_CMD_HALT + " " + CDKConstants.VAGRANT_FLAG_NO_COLOR; 
		
		Process p = null;
		try {
			p = new VagrantLaunchUtility().callInteractive(getServer(), cmd, getServer().getName());
		} catch(IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe);
			getBehavior().setServerStarted();
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}
		
		if( p == null ) {
			getBehavior().setServerStopped();
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to vagrant up has failed."));
		}

		linkTerminal(p);
		
		final Process p2 = p;
		Integer exitCode = ThreadUtils.runWithTimeout(600000, new Callable<Integer>() {
			@Override
		 	public Integer call() throws Exception {
				return p2.waitFor();
			}
		});
		
		if( exitCode == null ) {
			// Timeout reached
			p.destroyForcibly();
		}
		
		processTerminated(getServer(), null);
	}
	
	
	private void linkTerminal(Process p) {
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
	
	private void processTerminated(IServer server, IDebugEventSetListener listener) {
		final ControllableServerBehavior beh = (ControllableServerBehavior)JBossServerBehaviorUtils.getControllableBehavior(server);
		new Thread() {
			@Override
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
					beh.putSharedData(CDKServerBehaviour.PROP_CACHED_PASSWORD, null);
					beh.putSharedData(CDKServerBehaviour.PROP_CACHED_USER, null);
				} else {
					// The shutdown failed. We'll set the server to started and indicate a requiresForce
					beh.setServerStarted();
					setNextStopRequiresForce(true);
				}	
			}
		}.start();
		if( listener != null ) 
			DebugPlugin.getDefault().removeDebugEventListener(listener);
	}
}
