/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior;

import static org.jboss.tools.openshift.core.server.OpenShiftServerUtils.toCoreException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.ide.eclipse.as.core.util.ClassCollectingHCRListener;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ILaunchServerController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.ServerProcess;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.DockerImageLabels;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.behavior.ActivateMavenProfileJob.Action;
import org.jboss.tools.openshift.core.util.MavenCharacter;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.portforwarding.PortForwardingUtils;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.DebugLaunchConfigs;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.capability.resources.IPortForwardable.PortPair;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

/**
 * @author Rob Stryker
 * @author Fred Bricon
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class OpenShiftLaunchController extends AbstractSubsystemController
		implements ISubsystemController, ILaunchServerController {

	private static final int PUBLISH_DELAY = 3000;
	private static final int RECHECK_DELAY = 1000;
	private static final long WAIT_FOR_DEPLOYMENTCONFIG_TIMEOUT = 3 * 60 * 1024;
	private static final long WAIT_FOR_DOCKERIMAGELABELS_TIMEOUT = 3 * 60 * 1024;
	private static final long WAIT_FOR_NEW_DEBUG_POD_TIMEOUT = 10_000; // 10 seconds

	protected static final Map<IServer, IConnectionsRegistryListener> POD_LISTENERS = new HashMap<>();

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		OpenShiftServerBehaviour beh = OpenShiftServerUtils.getOpenShiftServerBehaviour(configuration);
		beh.setServerStarting();
		launchServerProcess(beh, launch, monitor);
		IProject project = OpenShiftServerUtils.getDeployProject(getServerOrWC());
		String currentMode = beh.getServer().getMode();

		Job toggleDebuggingAndSetState = new Job(NLS.bind("Setting up debugging for {0}", project.getName())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = toggleDebugging(mode, beh, monitor);
				setServerState(status, mode, currentMode, beh, monitor);
				return status;
			}
		};

		if (!new MavenCharacter(project).hasNature()) {
			toggleDebuggingAndSetState.schedule();
		} else {
			new JobChainBuilder(new ActivateMavenProfileJob(Action.ACTIVATE, project))
				.runWhenSuccessfullyDone(toggleDebuggingAndSetState)
				.schedule();
		}
	}

	private void setServerState(IStatus status, String mode, String currentMode,
			OpenShiftServerBehaviour beh, IProgressMonitor monitor) {
		if (!status.isOK()) {
			setServerState(beh, currentMode, monitor);
		} else {
			setServerState(beh, mode, monitor);
		}
	}				

	private IStatus toggleDebugging(String mode, OpenShiftServerBehaviour beh, IProgressMonitor monitor) {
		try {
			if (waitForDeploymentConfigReady(beh.getServer(), monitor)) {
				DebugContext context = createDebugContext(beh, monitor);
				toggleDebugging(mode, beh, context, monitor);
				setOpenShiftMode(mode, context, monitor);
				if (DebugLaunchConfigs.isDebugMode(mode)) {
				    createPodListener(beh, context, monitor);
				}
			}
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Could not launch server {0}", beh.getServer().getName()), e);
		}
	}

	protected void setOpenShiftMode(String mode, DebugContext context, IProgressMonitor monitor) throws CoreException {
		if (!DebugLaunchConfigs.isDebugMode(mode)) {
			// enable devmode if we're not in debug targetMode. Debug targetMode has dev targetMode enabled
			// anyhow
			new OpenShiftDebugMode(context).enableDevmode();
		}
		new OpenShiftDebugMode(context).execute(monitor);
	}

	protected void launchServerProcess(OpenShiftServerBehaviour beh, ILaunch launch, IProgressMonitor monitor) {
		launch.addProcess(new ServerProcess(launch, beh.getServer(), getLabel(launch.getLaunchMode())));
		beh.setServerStarting();
	}

	private String getLabel(String mode) {
		String label = null;

		switch (mode) {
		case ILaunchManager.DEBUG_MODE:
			label = OpenShiftCoreMessages.DebugOnOpenshift;
			break;
		case ILaunchManager.PROFILE_MODE:
			label = OpenShiftCoreMessages.ProfileOnOpenshift;
			break;
		default:
			label = OpenShiftCoreMessages.RunOnOpenshift;
			break;
		}
		return label;
	}

	protected boolean waitForDeploymentConfigReady(IServer server, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Waiting for deployment configs to become available...");

		Connection connection = OpenShiftServerUtils.getConnectionChecked(server);
		IResource resource = OpenShiftServerUtils.getResourceChecked(server, connection, monitor);
		long timeout = System.currentTimeMillis() + WAIT_FOR_DEPLOYMENTCONFIG_TIMEOUT;
		while ((ResourceUtils.getDeploymentConfigFor(resource, connection)) == null) {
			if (!sleep(RECHECK_DELAY, timeout, monitor)) {
				return false;
			}
		}
		return true;
	}

	protected boolean waitForDockerImageLabelsReady(DockerImageLabels metadata, IProgressMonitor monitor) {
		monitor.subTask("Waiting for docker image to become available...");
		long timeout = System.currentTimeMillis() + WAIT_FOR_DOCKERIMAGELABELS_TIMEOUT;
		while (!metadata.load(monitor)) {
			if (!sleep(RECHECK_DELAY, timeout, monitor)) {
				return false;
			}
		}
		return true;
	}

	private boolean sleep(int sleep, long timeout, IProgressMonitor monitor) {
		return sleep(sleep) && System.currentTimeMillis() < timeout && !monitor.isCanceled();
	}

	private boolean sleep(int sleep) {
		try {
			Thread.sleep(sleep);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	protected void toggleDebugging(String mode, OpenShiftServerBehaviour beh, DebugContext context,
			IProgressMonitor monitor) {
		boolean enableDebugging = DebugLaunchConfigs.isDebugMode(mode);
		if (enableDebugging) {
			startDebugging(beh, context, monitor);
		} else { //run, profile
			stopDebugging(context, monitor);
		}
	}

	protected DebugContext createDebugContext(OpenShiftServerBehaviour beh, IProgressMonitor monitor) {
		monitor.subTask("Initialising debugging...");

		DockerImageLabels imageLabels = getDockerImageLabels(beh, monitor);
		IServer server = beh.getServer();
		String devmodeKey = StringUtils.defaultIfBlank(OpenShiftServerUtils.getDevmodeKey(server),
				imageLabels.getDevmodeKey(monitor));
		String debugPortKey = StringUtils.defaultIfBlank(OpenShiftServerUtils.getDebugPortKey(server),
				imageLabels.getDevmodePortKey(monitor));
		String debugPort = StringUtils.defaultIfBlank(OpenShiftServerUtils.getDebugPort(server),
				imageLabels.getDevmodePortValue(monitor));
		return new DebugContext(beh.getServer(), devmodeKey, debugPortKey, debugPort);
	}

	private DockerImageLabels getDockerImageLabels(OpenShiftServerBehaviour beh, IProgressMonitor monitor) {
		IResource resource = OpenShiftServerUtils.getResource(beh.getServer(), monitor);
		DockerImageLabels metadata = DockerImageLabels.getInstance(resource, beh);
		waitForDockerImageLabelsReady(metadata, monitor);
		return metadata;
	}

	protected void setServerState(OpenShiftServerBehaviour beh, String mode, IProgressMonitor monitor) {
		int state = pollState(monitor);
		String currentMode = beh.getServer().getMode();

		if (!Objects.equals(currentMode, mode)) {
			setModulesPublishing();
			if (state == IServer.STATE_STARTED && beh.isRestarting()) {
				publishServer();
				beh.setRestarting(false);
			}
		}

		if (state == IServer.STATE_STARTED) {
			beh.setRunMode(mode);
			beh.setServerStarted();
		} else {
			beh.setServerStopped();
			beh.setRunMode(null);
		}
	}

	protected int pollState(IProgressMonitor monitor) {
		IResource resource = null;
		Exception e = null;
		resource = OpenShiftServerUtils.getResource(getServer(), monitor);
		if (resource == null) {
		    OpenShiftCoreActivator.pluginLog().logError(
					"The OpenShift resource for server " + getServer().getName() + " could not be reached.", e);
			return IServer.STATE_STOPPED;
		}
		return IServer.STATE_STARTED;
	}

	private void setModulesPublishing() {
		IModule[] modules = getServer().getModules();
		for (int i = 0; i < modules.length; i++) {
			((Server) getServer()).setModulePublishState(
					new IModule[] { modules[i] }, IServer.PUBLISH_STATE_FULL);
		}
	}

	private void publishServer() {
		new Job(NLS.bind("Publishing server {0}", getServer().getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return getServer().publish(IServer.PUBLISH_INCREMENTAL, monitor);
			}
		}.schedule(PUBLISH_DELAY);
	}

	protected void startDebugging(OpenShiftServerBehaviour behaviour, DebugContext context, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind("Starting debugging for server {0}", behaviour.getServer().getName()), 1);
		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebugContext context, IProgressMonitor monitor) throws CoreException {
				int localPort = startPortForwarding(context, monitor);
				ILaunch debuggerLaunch = attachRemoteDebugger(behaviour.getServer(), localPort, monitor);
				overrideHotcodeReplace(behaviour.getServer(), debuggerLaunch);
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor) throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		context.setDebugListener(listener);
		new OpenShiftDebugMode(context).enableDebugging();
		subMonitor.done();
	}

	protected void createPodListener(OpenShiftServerBehaviour beh, DebugContext context, IProgressMonitor monitor) {
	    IResource resource = OpenShiftServerUtils.getResource(context.getServer(), monitor);
	    IConnectionsRegistryListener podListener = new ConnectionsRegistryAdapter() {
	        private Timer stopDebugTimer;
            @Override
            public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
				if (newValue == null && oldValue instanceof IPod && oldValue.equals(context.getPod())) {
					stopDebugTimer = new Timer(context.getPod().getName());
					stopDebugTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							stopDebugging(context, monitor);
							setServerState(beh, ILaunchManager.RUN_MODE, monitor);
						}

					}, WAIT_FOR_NEW_DEBUG_POD_TIMEOUT);
				} else if (newValue instanceof IPod
						&& (ResourceUtils.isNewRuntimePodFor(
								(IPod) newValue, ResourceUtils.getDeploymentConfigFor(resource, (Connection) connection)))) {
					if (stopDebugTimer != null) {
						stopDebugTimer.cancel();
					}
					try {
						new OpenShiftDebugMode(context).execute(monitor);
					} catch (CoreException e) {
						OpenShiftCoreActivator.logError("Error occured while trying to launch debug on recovered pod",
								e);
					}
				}
            }
        };
        ConnectionsRegistrySingleton.getInstance().addListener(podListener);
        POD_LISTENERS.put(beh.getServer(), podListener);
	}

	private void stopDebugging(DebugContext context, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind("Stopping debugging for server {0}", context.getServer().getName()), 1);
		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebugContext context, IProgressMonitor monitor) throws CoreException {
				DebugLaunchConfigs configs = DebugLaunchConfigs.get();
				if( configs != null ) {
					configs.terminateRemoteDebugger(getServer());
				}
				unMapPortForwarding(context.getPod());
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor) throws CoreException {
				// nothing to do
			}
		};
		context.setDebugListener(listener);
		new OpenShiftDebugMode(context).disableDebugging();
		if (POD_LISTENERS.containsKey(context.getServer())) {
		    ConnectionsRegistrySingleton.getInstance().removeListener(POD_LISTENERS.remove(context.getServer()));
		}
		subMonitor.done();
	}

	@Override
	public IStatus canStart(String launchMode) {
		return Status.OK_STATUS;
	}

	@Override
	public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
			throws CoreException {
		// Do Nothing
	}

	/**
	 * Unmap the port forwarding. 
	 * @throws CoreException 
	 */
	private void unMapPortForwarding(IPod pod) throws CoreException {
		if (pod != null) {
			try {
				PortForwardingUtils.stopPortForwarding(pod, null);
			} catch (IOException e) {
				throw toCoreException("Unable to stop port forwarding", e);
			}
		}
	}

	/**
	 * Maps the remote port to a local port. 
	 * Return the local port in use, or -1 if failed
	 * @param server
	 * @param remotePort
	 * @return the local debug port or -1 if port forwarding did not start or was cancelled.
	 * @throws CoreException 
	 */
	protected int startPortForwarding(final DebugContext context, final IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Starting port forwarding...");

		IPod pod = context.getPod();
		if (pod == null) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, NLS.bind(
					"Could not find running pod to forward to in server adapter \"{0}\"", getServer().getName())));
		}
		Set<PortPair> podPorts = PortForwardingUtils.getForwardablePorts(pod);

		int remotePort = context.getDebugPort();
		if (remotePort == DebugContext.NO_DEBUG_PORT) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("No pod port to forward to specified in server adapter \"{0}\"", getServer().getName())));
		}

		PortPair debugPort = PortForwardingUtils.getPortPairForRemote(remotePort, podPorts);
		if (debugPort == null) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Pod port specified in server adapter \"{0}\" is not present in pod \"{1}\"",
							getServer().getName(), pod.getName())));
		}

		if (PortForwardingUtils.isPortForwardingStarted(pod)) {
			return debugPort.getLocalPort();
		}

		if (PortForwardingUtils.setLocalPortsTo(podPorts, monitor)) {
			PortForwardingUtils.startPortForwarding(pod, podPorts, IBinaryCapability.SKIP_TLS_VERIFY);
			if (PortForwardingUtils.isPortForwardingStarted(pod)) {
				return debugPort.getLocalPort();
			}
		}

		throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
				NLS.bind("Could not setup port forwarding to pod \"{0}\" in server adapter \"{1}\"", pod.getName(),
						getServer().getName())));
	}

	private ILaunch attachRemoteDebugger(IServer server, int localDebugPort, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask("Attaching remote debugger...");
		ILaunch ret = null;
		DebugLaunchConfigs launchConfigs = DebugLaunchConfigs.get();
		if (launchConfigs == null) {
			throw toCoreException(
					NLS.bind("Could not get launch config for server {0} to attach remote debugger", server.getName()));
		}
		ILaunchConfiguration debuggerLaunchConfig = launchConfigs.getRemoteDebuggerLaunchConfiguration(server);
		ILaunchConfigurationWorkingCopy workingCopy = launchConfigs.getLaunchConfigWorkingCopy(server, debuggerLaunchConfig);
		if (workingCopy == null) {
			throw toCoreException(NLS.bind("Could not modify launch config for server {0}", server.getName()));
		}
		IProject project = OpenShiftServerUtils.getDeployProject(server);
		launchConfigs.setupRemoteDebuggerLaunchConfiguration(workingCopy, project, localDebugPort);
		debuggerLaunchConfig = workingCopy.doSave();

		ret = launchConfigs.lauchDebugger(debuggerLaunchConfig, localDebugPort, monitor);
		if (ret == null) {
			throw toCoreException(
					NLS.bind("Could not start remote debugger to (forwarded) port {0} on localhost", localDebugPort));
		}

		monitor.worked(10);
		return ret;
	}

	protected boolean overrideHotcodeReplace(IServer server, ILaunch launch) {
		IJavaHotCodeReplaceListener l = getHotCodeReplaceListener(server, launch);
		IDebugTarget[] targets = launch.getDebugTargets();
		if (targets != null && l != null) {
			for (int i = 0; i < targets.length; i++) {
				if (targets[i] instanceof IJavaDebugTarget) {
					((IJavaDebugTarget) targets[i]).addHotCodeReplaceListener(l);
				}
			}
		}
		return true;
	}

	protected IJavaHotCodeReplaceListener getHotCodeReplaceListener(final IServer server, final ILaunch launch) {
		return new ClassCollectingHCRListener(server, launch) {

			protected void prePublish(IJavaDebugTarget target, IModule[] modules) {
				try {
					getLaunch().terminate();
				} catch (DebugException de) {
					OpenShiftCoreActivator.pluginLog()
							.logError(toCoreException("Unable to terminate debug session", de));
				}
			}

			@Override
			protected void postPublish(IJavaDebugTarget target, IModule[] modules) {
				IServer server = getServer();
				waitModulesStarted(modules);
				executeJMXGarbageCollection(server, modules);

				sleep(3000);

				String portAttr = launch.getAttribute(DebugLaunchConfigs.LAUNCH_DEBUG_PORT_PROP);
				int port = DebugContext.NO_DEBUG_PORT;
				try {
					port = Integer.parseInt(portAttr);
				} catch (NumberFormatException nfe) {
					// TODO: handle non numerical port value (named port) 
				}
				try {
					ILaunch newLaunch = attachRemoteDebugger(server, port, new NullProgressMonitor());
					if (newLaunch != null) {
						overrideHotcodeReplace(server, newLaunch);
					}
					setLaunch(newLaunch);
				} catch (CoreException ce) {
					OpenShiftCoreActivator.pluginLog().logError(toCoreException("Unable to restart debug session", ce));
				}
			}
		};
	}
}
