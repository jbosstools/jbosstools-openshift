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
package org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers;

import java.io.IOException;
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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
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
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.AbstractCDKPoller;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServerBehaviour;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;

public abstract class AbstractCDKShutdownController extends AbstractSubsystemController
		implements IServerShutdownController, IExternalLaunchConstants {

	protected IServerStatePoller2 getPoller(IServer server) {
		return getCDKPoller(server);
	}
	@Deprecated
	protected AbstractCDKPoller getCDKPoller(IServer server) {
		return null;
	}

	protected abstract String getShutdownArgs();

	protected abstract Process call(IServer s, String args, String launchConfigName) throws CoreException, IOException;

	@Override
	public IStatus canStop() {
		return Status.OK_STATUS;
	}

	public ControllableServerBehavior getBehavior() {
		return (ControllableServerBehavior) getServer().loadAdapter(ControllableServerBehavior.class,
				new NullProgressMonitor());
	}

	private void cancelPoller() {
		// Server is in process of starting or stopping... 
		Object pt = getBehavior().getSharedData(IDeployableServerBehaviorProperties.POLL_THREAD);
		if (pt instanceof PollThread) {
			((PollThread) pt).cancel();
		}
	}

	protected boolean getRequiresForce() {
		Object o = getControllableBehavior()
				.getSharedData(AbstractStartJavaServerLaunchDelegate.NEXT_STOP_REQUIRES_FORCE);
		return o == null ? false : ((Boolean) o).booleanValue();
	}

	protected void setNextStopRequiresForce(boolean val) {
		getControllableBehavior().putSharedData(AbstractStartJavaServerLaunchDelegate.NEXT_STOP_REQUIRES_FORCE, val);
	}

	protected void pollState() {
		IStatus state = PollThreadUtils.isServerStarted(getServer(), getPoller(getServer()));
		boolean started = state.isOK();
		if (!started) {
			if (state.getSeverity() == IStatus.ERROR) {
				// server is stopped, cancel the poller
				cancelPoller();
				getBehavior().setServerStopped();
				return;
			}
		}
	}

	@Override
	public void stop(boolean force) {
		getBehavior().setServerStopping();
		pollState();
		if (getServer().getServerState() == IServer.STATE_STOPPED) {
			return;
		}
		issueShutdownCommand();
	}

	protected void issueShutdownCommand() {
		try {
			if (useTerminal())
				shutdownViaTerminal();
			else
				shutdownViaLaunch();
		} catch (CoreException ce) {
			CDKCoreActivator.getDefault().getLog()
					.log(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Error shutting down server", ce));
			getBehavior().setServerStarted();
		}
	}

	protected abstract boolean useTerminal();

	protected abstract String getCommandLocation();

	private void shutdownViaTerminal() throws CoreException {
		String args = getShutdownArgs();

		Process p = null;
		try {
			p = call(getServer(), args, getServer().getName());
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe);
			getBehavior().setServerStarted();
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ioe.getMessage(), ioe));
		}

		if (p == null) {
			getBehavior().setServerStopped();
			throw new CoreException(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to shutdown command has failed."));
		}

		linkTerminal(p);

		final Process p2 = p;
		Integer exitCode = ThreadUtils.runWithTimeout(600000, new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return p2.waitFor();
			}
		});

		if (exitCode == null) {
			// Timeout reached
			p.destroyForcibly();
		}

		processTerminated(getServer(), null);
	}

	protected ILaunchConfigurationWorkingCopy createShutdownLaunchConfiguration(ILaunchConfiguration lc, String cmd,
			String args) throws CoreException {
		CDKServer cdk = (CDKServer)getServer().loadAdapter(CDKServer.class, new NullProgressMonitor());
		ILaunchConfigurationWorkingCopy lc2 = new CDKLaunchUtility().createExternalToolsLaunch(getServer(), args,
				new Path(cmd).lastSegment(), lc, cmd, cdk.skipUnregistration());
		return lc2;
	}

	private void shutdownViaLaunch() throws CoreException {
		String args = getShutdownArgs();
		String cmd = getCommandLocation();
		ILaunchConfiguration lc = getServer().getLaunchConfiguration(true, new NullProgressMonitor());
		ILaunchConfigurationWorkingCopy lc2 = createShutdownLaunchConfiguration(lc, cmd, args);
		
		IProcess p = null;
		ILaunch launch = null;
		try {
			launch = lc2.launch("run", new NullProgressMonitor());
			IProcess[] all = launch.getProcesses();
			if (all.length > 0)
				p = all[0];
		} catch (CoreException ce) {
			ce.printStackTrace();
			CDKCoreActivator.pluginLog().logError(ce);
			getBehavior().setServerStarted();
			throw new CoreException(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, ce.getMessage(), ce));
		}

		if (p == null) {
			getBehavior().setServerStarted();
			throw new CoreException(
					new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Call to shutdown command has failed."));
		}
		final IProcess myProcess = p;

		IDebugEventSetListener listener = (new IDebugEventSetListener() {
			public void handleDebugEvents(DebugEvent[] events) {
				if (events != null) {
					int size = events.length;
					for (int i = 0; i < size; i++) {
						if (myProcess != null && myProcess.equals(events[i].getSource())
								&& events[i].getKind() == DebugEvent.TERMINATE) {
							processTerminated(getServer(), null);
							DebugPlugin.getDefault().removeDebugEventListener(this);
						}
					}
				}
			}
		});
		DebugPlugin.getDefault().addDebugEventListener(listener);
	}

	private void linkTerminal(Process p) {
		ProcessLaunchUtility.linkTerminal(getServer(), p);
	}

	private void processTerminated(IServer server, IDebugEventSetListener listener) {
		final ControllableServerBehavior beh = (ControllableServerBehavior) JBossServerBehaviorUtils
				.getControllableBehavior(server);
		new Thread() {
			@Override
			public void run() {
				try {
					// sleep to allow vagrant to unlock queries. 
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
				}

				// Poll the server once more 
				IStatus stat = getPoller(server).getCurrentStateSynchronous(getServer());
				if (stat.getSeverity() == IStatus.ERROR) {
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
		if (listener != null)
			DebugPlugin.getDefault().removeDebugEventListener(listener);
	}
}
