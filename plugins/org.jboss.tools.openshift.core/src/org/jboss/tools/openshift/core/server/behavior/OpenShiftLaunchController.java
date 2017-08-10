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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.jdt.launching.SocketUtil;
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
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.core.server.DockerImageLabels;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.portforwarding.PortForwardingUtils;
import org.jboss.tools.openshift.internal.core.server.debug.DebugLaunchConfigs;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode.DebugContext;

import com.openshift.restclient.capability.IBinaryCapability.OpenShiftBinaryOption;
import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.capability.resources.IPortForwardable.PortPair;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

/**
 * @author Rob Stryker
 * @author Fred Bricon
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class OpenShiftLaunchController extends AbstractSubsystemController implements ISubsystemController, ILaunchServerController {

	private static final String LAUNCH_DEBUG_PORT_PROP = "LOCAL_DEBUG_PORT";
	private static final int RECHECK_DELAY = 1000;
	private static final int PUBLISH_DELAY = 3000;
	private static final int DEBUGGER_LAUNCHED_TIMEOUT =  60_000; //TODO Get from server settings?
	private static final String DEBUG_MODE = "debug"; //$NON-NLS-1$
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		OpenShiftServerBehaviour beh = OpenShiftServerUtils.getOpenShiftServerBehaviour(configuration);
		String currentMode = beh.getServer().getMode();
		beh.setServerStarting();
		launchServerProcess(beh, launch, monitor);
		try {
			if (waitForDeploymentConfigReady(beh.getServer(), monitor)) {
				DebugContext context = createDebugContext(beh, monitor);
				toggleDebugging(mode, beh, context, monitor);
				OpenShiftDebugMode.sendChanges(context, monitor);
			}
		} catch (Exception e) {
			mode = currentMode;
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Could not launch server {0}", beh.getServer().getName()), e));
		} finally {
			setServerState(beh, mode, monitor);
		}
	}

	protected void launchServerProcess(OpenShiftServerBehaviour beh, ILaunch launch,
			IProgressMonitor monitor) {
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
		while ((OpenShiftServerUtils.getDeploymentConfig(server, monitor)) == null 
				&& monitor.isCanceled()) {
			sleep(RECHECK_DELAY);
		}
		return true;
	}

	protected boolean waitForDockerImageLabelsReady(DockerImageLabels metadata, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("waiting for docker image to become available...");
		while (!monitor.isCanceled()) {
			while (!metadata.load() 
					&& sleep(RECHECK_DELAY)) {
				}
				return true;
		}
		return false;
	}

	private boolean sleep(int sleep) {
		try {
			Thread.sleep(sleep);
			return true;
		} catch(InterruptedException ie) {
			return false;
		}
	}

	protected void toggleDebugging(String mode, OpenShiftServerBehaviour beh, DebugContext context, IProgressMonitor monitor) {
		boolean enableDebugging = isDebugMode(mode);
		if (enableDebugging) {
			startDebugging(beh, context, monitor);
		} else { //run, profile
			stopDebugging(context, monitor);
		}
	}

	protected DebugContext createDebugContext(OpenShiftServerBehaviour beh, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Initialising debugging...");
		DockerImageLabels imageLabels = getDockerImageLabels(beh, monitor);
		String devmodeKey = getDevmodeKey(beh.getServer(), imageLabels);
		String debugPortKey = getDebugPortKey(beh.getServer(), imageLabels);
		String debugPort = getDebugPort(beh.getServer(), imageLabels);
		DebugContext debugContext = OpenShiftDebugMode.createContext(beh, devmodeKey, debugPortKey, debugPort);
		return debugContext;
	}

	private DockerImageLabels getDockerImageLabels(OpenShiftServerBehaviour beh, IProgressMonitor monitor) throws CoreException {
		IResource resource = OpenShiftServerUtils.getResource(beh.getServer(), monitor);
		DockerImageLabels metadata = DockerImageLabels.getInstance(resource, beh);
		waitForDockerImageLabelsReady(metadata, monitor);
		return metadata;
	}

	private static String getDevmodeKey(IServer server, DockerImageLabels metadata) throws CoreException {
		String devmodeKey = OpenShiftServerUtils.getDevmodeKey(server);
		if (StringUtils.isBlank(devmodeKey)) {
			devmodeKey = metadata.getDevmodeKey();
		}
		return devmodeKey;
	}

	private static String getDebugPortKey(IServer server, DockerImageLabels metadata) throws CoreException {
		String debugPortKey = OpenShiftServerUtils.getDebugPortKey(server);
		if (StringUtils.isBlank(debugPortKey)) {
			debugPortKey = metadata.getDevmodePortKey();
		}
		return debugPortKey;
	}

	private static String getDebugPort(IServer server, DockerImageLabels metadata) throws CoreException {
		String debugPort = OpenShiftServerUtils.getDebugPort(server);
		if (StringUtils.isBlank(debugPort)) {
			debugPort = metadata.getDevmodePortValue();
		}
		return debugPort;
	}

	protected void setServerState(OpenShiftServerBehaviour beh, String mode, IProgressMonitor monitor) {
		int state = pollState(monitor);
		String currentMode = beh.getServer().getMode();

		if (!Objects.equals(currentMode, mode)) {
			setModulesPublishing();
			if (state == IServer.STATE_STARTED
					&& beh.isRestarting()) {
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

	protected boolean isDebugMode() {
		return isDebugMode(getServer().getMode());
	}
	
	protected boolean isDebugMode(String mode) {
		return DEBUG_MODE.equals(mode);
	}

	private void setModulesPublishing() {
		IModule[] modules = getServer().getModules();
		for (int i = 0; i < modules.length; i++) {
			((Server) getServer()).setModulePublishState(
					new IModule[] { modules[i] }, IServer.PUBLISH_STATE_FULL);
		}
	}
	
	private void publishServer() {
		new Job("Publishing server " + getServer().getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return getServer().publish(IServer.PUBLISH_INCREMENTAL, monitor);
			}
		}.schedule(PUBLISH_DELAY);
	}

	protected void startDebugging(OpenShiftServerBehaviour behaviour, DebugContext context,
			IProgressMonitor monitor) {
		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebugContext context, IProgressMonitor monitor)
					throws CoreException {
				int localPort = mapPortForwarding(context, monitor);
				IServer server = behaviour.getServer();
				ILaunch debuggerLaunch = attachRemoteDebugger(server, localPort, monitor);
				if (debuggerLaunch != null) {
					overrideHotcodeReplace(server, debuggerLaunch);
				}
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		context.setDebugListener(listener);
		OpenShiftDebugMode.enableDebugging(context);
	}

	private void stopDebugging(DebugContext debugContext, IProgressMonitor monitor) {
		IDebugListener listener = new IDebugListener() {
			
			@Override
			public void onDebugChange(DebugContext context, IProgressMonitor monitor)
					throws CoreException {
				DebugLaunchConfigs.get().terminateRemoteDebugger(getServer());
				unMapPortForwarding(context.getPod());
			}

			@Override
			public void onPodRestart(DebugContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
			}
		};
		debugContext.setDebugListener(listener);
		OpenShiftDebugMode.disableDebugging(debugContext);
	}

	@Override
	public IStatus canStart(String launchMode) {
		return Status.OK_STATUS;
	}

	@Override
	public void setupLaunchConfiguration(
			ILaunchConfigurationWorkingCopy workingCopy,
			IProgressMonitor monitor) throws CoreException {
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
	 * Map the remote port to a local port. 
	 * Return the local port in use, or -1 if failed
	 * @param server
	 * @param remotePort
	 * @return the local debug port or -1 if port forwarding did not start or was cancelled.
	 * @throws CoreException 
	 */
	protected int mapPortForwarding(final DebugContext context, final IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Starting port forwarding...");

		IPod pod = context.getPod();
		if (pod == null) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
					NLS.bind("Could not find running pod to forward to in server adapter \"{0}\"", getServer().getName())));
		}
		Set<PortPair> podPorts = PortForwardingUtils.getForwardablePorts(pod);

		int remotePort = context.getDebugPort();
		if (remotePort == DebugContext.NO_DEBUG_PORT) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
					NLS.bind("No pod port to forward to specified in server adapter \"{0}\"", getServer().getName())));
		}

		Optional<PortPair> debugPort = podPorts.stream()
				.filter(p -> remotePort == p.getRemotePort())
				.findFirst();		
		if (!debugPort.isPresent()) {
			throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
					NLS.bind("Pod port specified in server adapter \"{0}\" is no present in pod \"{1}\"", getServer().getName(), pod.getName())));
		}

		if (PortForwardingUtils.isPortForwardingStarted(pod)) {
			return debugPort.get().getLocalPort();
		} 

		if (mapPorts(podPorts, monitor)) {		
			PortForwardingUtils.startPortForwarding(pod, podPorts, OpenShiftBinaryOption.SKIP_TLS_VERIFY);			
			if (PortForwardingUtils.isPortForwardingStarted(pod)) {
				return debugPort.get().getLocalPort();
			}
		}

		throw new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
				NLS.bind("Could not setup port forwarding to pod \"{0}\" in server adapter \"{1}\"", pod.getName(), getServer().getName())));
	}

	private boolean mapPorts(Set<PortPair> podPorts, final IProgressMonitor monitor) {
		for (IPortForwardable.PortPair port : podPorts) {
			if (monitor.isCanceled()) {
				return false;
			}
			port.setLocalPort(SocketUtil.findFreePort());
			monitor.worked(1);
		}
		return true;
	}
	
	private ILaunch attachRemoteDebugger(IServer server, int localDebugPort, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Attaching remote debugger...");
		ILaunch ret = null;
		DebugLaunchConfigs launchConfigs = DebugLaunchConfigs.get();
		ILaunchConfiguration debuggerLaunchConfig = launchConfigs.getRemoteDebuggerLaunchConfiguration(server);
		ILaunchConfigurationWorkingCopy workingCopy = getLaunchConfigWorkingCopy(server, launchConfigs, debuggerLaunchConfig);
		if (workingCopy == null) {
			throw toCoreException(NLS.bind("Could not modify launch config for server {0}", server.getName()));
		}
		IProject project = OpenShiftServerUtils.getDeployProject(server);
		launchConfigs.setupRemoteDebuggerLaunchConfiguration(workingCopy, project, localDebugPort);
		debuggerLaunchConfig = workingCopy.doSave();
		
		ret = lauchDebugger(debuggerLaunchConfig, localDebugPort, monitor);
		if (ret == null) {
			throw toCoreException(NLS.bind("Could not start remote debugger to (forwarded) port {0} on localhost", localDebugPort));
		}
		
	    monitor.worked(10);
	    return ret;
	}

	private ILaunch lauchDebugger(ILaunchConfiguration debuggerLaunchConfig, int port, IProgressMonitor monitor) {
		ILaunch launch = null;
		int elapsed = 0;
		boolean launched = false;
		monitor.subTask("Waiting for remote debug port to become available...");
		while(!launched 
				&& elapsed < DEBUGGER_LAUNCHED_TIMEOUT) {
			try {
				//TODO That's fugly. ideally we should see if socket on debug port is responsive instead
				launch = debuggerLaunchConfig.launch(DEBUG_MODE, new NullProgressMonitor());
				launch.setAttribute(LAUNCH_DEBUG_PORT_PROP, Integer.toString(port));
				launched = true;
			} catch (Exception e) {
				if (monitor.isCanceled()) {
					break;
				}
				try {
					Thread.sleep(RECHECK_DELAY);
					elapsed += RECHECK_DELAY;
				} catch (InterruptedException ie) {
				}
			}
	    }
		return launch;
	}
	
	private ILaunchConfigurationWorkingCopy getLaunchConfigWorkingCopy(IServer server, DebugLaunchConfigs launchConfigs,
			ILaunchConfiguration debuggerLaunchConfig) throws CoreException {
		ILaunchConfigurationWorkingCopy workingCopy;
		if (debuggerLaunchConfig == null) {
			workingCopy = launchConfigs.createRemoteDebuggerLaunchConfiguration(server);
		} else {
			if (launchConfigs.isRunning(debuggerLaunchConfig)) {
				return null;
			}
			workingCopy = debuggerLaunchConfig.getWorkingCopy();
		}
		return workingCopy;
	}

	protected boolean overrideHotcodeReplace(IServer server, ILaunch launch) throws CoreException {
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
				} catch(DebugException de) {
					OpenShiftCoreActivator.pluginLog().logError(toCoreException("Unable to terminate debug session", de));
				}
			}
			
			@Override
			protected void postPublish(IJavaDebugTarget target, IModule[] modules) {
				IServer server = getServer();
				waitModulesStarted(modules);
				executeJMXGarbageCollection(server, modules);
				
				sleep(3000);
				
				String portAttr = launch.getAttribute(LAUNCH_DEBUG_PORT_PROP);
				int port = DebugContext.NO_DEBUG_PORT;
				try {
					port = Integer.parseInt(portAttr);
				} catch(NumberFormatException nfe) {
					// TODO 
				}
				try {
					ILaunch newLaunch = attachRemoteDebugger(server, port, new NullProgressMonitor());
					if( newLaunch != null ) {
						overrideHotcodeReplace(server, newLaunch);
					}
					setLaunch(newLaunch);
				} catch(CoreException ce) {
					OpenShiftCoreActivator.pluginLog().logError(toCoreException("Unable to restart debug session", ce));
				}
			}
		};
	}
}
