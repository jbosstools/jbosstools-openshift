/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.debug;

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.wtp.core.server.launch.ServerHotCodeReplaceListener;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.portforwarding.PortForwardingUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.IClient;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability.OpenShiftBinaryOption;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.capability.resources.IPortForwardable;
import com.openshift.restclient.capability.resources.IPortForwardable.PortPair;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

public class OpenShiftDebugUtils {
	public static final String DEBUG_MODE = "debug";

	private static final String DEBUG_KEY = "DEBUG";
	private static final String DEV_MODE_KEY = "DEV_MODE";
	private static final String DEBUG_PORT_KEY = "DEBUG_PORT";
	private ILaunchManager launchManager;

	public static OpenShiftDebugUtils get() {
		return get(DebugPlugin.getDefault().getLaunchManager());
	}
	
	/** For testing purposes **/
	public static OpenShiftDebugUtils get(ILaunchManager launchManager) {
		return new OpenShiftDebugUtils(launchManager);
	}
	
	private OpenShiftDebugUtils(ILaunchManager launchManager) {
		this.launchManager = launchManager;
	}
	
	public DebuggingContext enableDebugMode(IDeploymentConfig deploymentConfig, DebuggingContext debugContext, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(deploymentConfig);
		Assert.isNotNull(debugContext);

		IDebugListener listener = debugContext.getDebugListener();
		if (debugContext.isDebugEnabled() && listener != null) {
			IPod pod = getFirstPod(deploymentConfig);
			debugContext.setPod(pod);
			listener.onDebugChange(debugContext, monitor);
		} else {
			debugContext.setDebugEnabled(true);
			updateDebugConfig(deploymentConfig, debugContext, true, monitor);
		}
		return debugContext;
	}

	public DebuggingContext disableDebugMode(IDeploymentConfig deploymentConfig, DebuggingContext debugContext, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(deploymentConfig);
		Assert.isNotNull(debugContext);

		IDebugListener listener = debugContext.getDebugListener();
		if (listener != null) {
			IPod pod = getFirstPod(deploymentConfig);
			debugContext.setPod(pod);
			listener.onDebugChange(debugContext, monitor);
		} 
		if (debugContext.isDebugEnabled()) {
			debugContext.setDebugEnabled(false);
			updateDebugConfig(deploymentConfig, debugContext, false, monitor);
		}
		return debugContext;
	}
	
	private void updateDebugConfig(IDeploymentConfig deploymentConfig, DebuggingContext debugContext, boolean enabling, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Updating Deployment Configuration");
		if (deploymentConfig == null
				|| deploymentConfig.getEnvironmentVariables() == null) {
			return;
		}

		if(enabling) {
			updateDeploymentConfigValuesOnEnable(deploymentConfig, debugContext);
		} else {
			updateDeploymentConfigValuesOnDisable(deploymentConfig, debugContext);
		}
		
		IClient client = getClient(deploymentConfig);
		
		DeploymentConfigListenerJob deploymentListenerJob = new DeploymentConfigListenerJob(deploymentConfig);
		deploymentListenerJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				ConnectionsRegistrySingleton.getInstance().removeListener(deploymentListenerJob.getConnectionsRegistryListener());
				debugContext.setPod(deploymentListenerJob.getPod());
				if (event.getResult().isOK() && debugContext.getDebugListener() != null) {
					try {
						debugContext.getDebugListener().onPodRestart(debugContext, monitor);
					} catch (CoreException e) {
						throw new OpenShiftCoreException(e);
					}
				}
			};
		});
		
		ConnectionsRegistrySingleton.getInstance().addListener(deploymentListenerJob.getConnectionsRegistryListener());
		deploymentListenerJob.schedule();
		client.update(deploymentConfig);
		try {
			deploymentListenerJob.join(DeploymentConfigListenerJob.TIMEOUT, monitor);
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
		IStatus result = deploymentListenerJob.getResult();
		if (result == null) {//timed out!
			throw new CoreException(deploymentListenerJob.getTimeOutStatus());
		} else if (!result.isOK()) {
			throw new CoreException(result);
		}
		
	}

	private void updateDeploymentConfigValuesOnEnable(IDeploymentConfig deploymentConfig,
			DebuggingContext debugContext) {
		OriginalDeployConfigInfo originalInfo = new OriginalDeployConfigInfo();
		originalDeploymentConfigs.put(getDeploymentConfigKey(deploymentConfig), originalInfo);
		originalInfo.debugPort = getEnv(deploymentConfig, DEBUG_PORT_KEY);
		originalInfo.devMode = getEnv(deploymentConfig, DEV_MODE_KEY);
		originalInfo.debug = getEnv(deploymentConfig, DEBUG_KEY);

		Collection<IContainer> originalContainers = deploymentConfig.getContainers();
		if (originalContainers != null && !originalContainers.isEmpty()) {
			Collection<IContainer> containers = new ArrayList<>(originalContainers);
			IContainer container = containers.iterator().next();
			Set<IPort> ports = new HashSet<>(container.getPorts());

			IPort existing = ports.stream().filter(p -> p.getContainerPort() == debugContext.getDebugPort())
										.findFirst().orElse(null);
			boolean added = false;
			if (existing == null) {
				PortSpecAdapter newPort = new PortSpecAdapter("debug", "TCP", debugContext.getDebugPort());
				added = ports.add(newPort);
				if(added) {
					originalInfo.updatedPort = newPort;
				}
			} else {
				PortSpecAdapter newPort = new PortSpecAdapter(existing.getName(), existing.getProtocol(), debugContext.getDebugPort());
				if (!existing.equals(newPort)) {
					ports.remove(existing);
					originalInfo.originalPort = existing;
					added = ports.add(newPort);
					if(added) {
						originalInfo.updatedPort = newPort;
					}
				}
			}
			if (added) {
				originalInfo.containerName = container.getName();
				container.setPorts(ports);
				deploymentConfig.setContainers(containers);
			}
		}
		deploymentConfig.setEnvironmentVariable(DEBUG_PORT_KEY, String.valueOf(debugContext.getDebugPort()));
		deploymentConfig.setEnvironmentVariable(DEV_MODE_KEY, String.valueOf(debugContext.isDebugEnabled()));//for node
		deploymentConfig.setEnvironmentVariable(DEBUG_KEY, String.valueOf(debugContext.isDebugEnabled()));//for eap
		//TODO the list of env var to set in debug mode should probably be defined in the server settings instead
	}

	private void updateDeploymentConfigValuesOnDisable(IDeploymentConfig deploymentConfig,
			DebuggingContext debugContext) {
		OriginalDeployConfigInfo originalInfo = originalDeploymentConfigs.remove(getDeploymentConfigKey(deploymentConfig));
		if(originalInfo == null) {
			//normally should not happen.
			originalInfo = new OriginalDeployConfigInfo();
		}
		Collection<IContainer> originalContainers = deploymentConfig.getContainers();
		if (originalInfo.containerName != null && originalContainers != null && !originalContainers.isEmpty()) {
			Collection<IContainer> containers = new ArrayList<>(originalContainers);
			final String containerName = originalInfo.containerName;
			IContainer container = containers.stream().filter(c -> containerName.equals(c.getName())).findFirst().orElse(null);
			if(container != null) {
				Set<IPort> ports = new HashSet<>(container.getPorts());
				boolean changed = false;
				if(originalInfo.updatedPort != null) {
					changed = ports.remove(originalInfo.updatedPort);
				}
				if(originalInfo.originalPort != null) {
					changed |= ports.add(originalInfo.originalPort);
				}
				if (changed) {
					container.setPorts(ports);
					deploymentConfig.setContainers(containers);
				}
			}
		}
		deploymentConfig.setEnvironmentVariable(DEBUG_PORT_KEY, originalInfo.debugPort);
		deploymentConfig.setEnvironmentVariable(DEV_MODE_KEY, originalInfo.devMode);//for node
		deploymentConfig.setEnvironmentVariable(DEBUG_KEY, originalInfo.debug);//for eap
	}

	private String getDeploymentConfigKey(IDeploymentConfig deploymentConfig) {
		return new StringBuilder().append(deploymentConfig.getNamespace()).append('/').append(deploymentConfig.getName()).toString();
	}

	private static class OriginalDeployConfigInfo {
		//env vars
		String debugPort;
		String devMode;
		String debug;

		//container and port
		String containerName;
		IPort originalPort;
		PortSpecAdapter updatedPort;
	}
	private static Map<String, OriginalDeployConfigInfo> originalDeploymentConfigs = new HashMap<>();

	private IClient getClient(IDeploymentConfig deploymentConfig) {
		IClient client = deploymentConfig.accept(new CapabilityVisitor<IClientCapability, IClient>() {
			@Override
			public IClient visit(IClientCapability cap) {
				return cap.getClient();
			}
		}, null);
		return client;
	}

	public DebuggingContext getDebuggingContext(IDeploymentConfig deploymentConfig) {
		if (deploymentConfig == null) {
			return null;
		}
		DebuggingContext debugContext = new DebuggingContext();
		String debugPort = getEnv(deploymentConfig, DEBUG_PORT_KEY);
		debugContext.setDebugPort(NumberUtils.toInt(debugPort, -1));
		String debugEnabled = getEnv(deploymentConfig, DEBUG_KEY);
		String devModeEnabled = getEnv(deploymentConfig, DEV_MODE_KEY);
		debugContext.setDebugEnabled(Boolean.parseBoolean(debugEnabled) || Boolean.parseBoolean(devModeEnabled));
		return debugContext;
	}
	
	public String getEnv(IDeploymentConfig deploymentConfig, String key) {
		if (deploymentConfig == null || deploymentConfig.getEnvironmentVariables() == null) {
			return null;
		}
		Optional<IEnvironmentVariable> envVar = deploymentConfig.getEnvironmentVariables().stream()
				.filter(ev -> key.equals(ev.getName()))
				.findFirst();
		if (envVar.isPresent()) {
			return envVar.get().getValue();
		}
		return null;
	}
	
	public IPod getFirstPod(IDeploymentConfig dc) {
		IPod pod = ResourceUtils.getPodsForDeploymentConfig(dc).stream().findFirst() 
				.orElse(null);
		return pod;
	}
	
	public ILaunchConfiguration getRemoteDebuggerLaunchConfiguration(IServer server) throws CoreException {
		ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION);
		ILaunchConfiguration[] launchConfigs = launchManager.getLaunchConfigurations(launchConfigurationType);
		String name = getRemoteDebuggerLaunchConfigurationName(server);
		Optional<ILaunchConfiguration> maybeLaunch = Stream.of(launchConfigs)
				.filter(lc -> name.equals(lc.getName()))
				.findFirst();
		
		return maybeLaunch.orElse(null);
	}
	
	public ILaunchConfigurationWorkingCopy createRemoteDebuggerLaunchConfiguration(IServer server) throws CoreException {
		String name = getRemoteDebuggerLaunchConfigurationName(server);
		ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION);
		ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, name);
		return workingCopy;
	}
	
	public void setupRemoteDebuggerLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProject project, int debugPort) throws CoreException {
		String portString = String.valueOf(debugPort);
	    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);
		Map<String, String> connectMap = new HashMap<>(2);
		connectMap.put("port", portString); //$NON-NLS-1$
		connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);
		if(project != null) {
		   workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		}
	}
	
	public boolean isRunning(ILaunchConfiguration launchConfiguration, int localDebugPort) {
		boolean isRunning = getLaunches()
				.filter(l -> !l.isTerminated() && launchMatches(l, launchConfiguration, localDebugPort))
				.findFirst().isPresent();
		return isRunning;
	}
	
	
	private boolean launchMatches(ILaunch l, ILaunchConfiguration launchConfiguration, int localDebugPort) {
		return Objects.equals(l.getLaunchConfiguration(), launchConfiguration);
	}

	public static String getRemoteDebuggerLaunchConfigurationName(IServer server) {
		String name ="Remote debugger to "+server.getName();
		return name;
	}
	
	public void terminateRemoteDebugger(IServer server) throws CoreException {
		ILaunchConfiguration launchConfig = getRemoteDebuggerLaunchConfiguration(server);
		if (launchConfig == null) {
			return;
		}
		List<IStatus> errors = new ArrayList<>();
		getLaunches().filter(l -> launchConfig.equals(l.getLaunchConfiguration()))
					 .filter(l -> l.canTerminate())
					 .forEach(l -> terminate(l, errors));
		
		if (!errors.isEmpty()) {
			MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, IStatus.ERROR, errors.toArray(new IStatus[errors.size()]), "Failed to terminate remote launch configuration", null);
			throw new CoreException(status);
		}
	}
	
	private void terminate(ILaunch launch, Collection<IStatus> errors) {
		try {
			launch.terminate();
		} catch (DebugException e) {
			errors.add(e.getStatus());
		}
	}

	private Stream<ILaunch> getLaunches() {
		return Stream.of(launchManager.getLaunches());
	}

	public void startDebugging(IServer server, IProgressMonitor monitor) throws CoreException {
		IDeploymentConfig dc = OpenShiftServerUtils.getDeploymentConfig(server);
		if (dc == null) {
			throw toCoreException(NLS.bind("Could not find deployment config was for {0}. "
					+ "Your server adapter refers to an inexistant service"
					+ ", there are no pods for it "
					+ "or there are no labels on those pods pointing to the wanted deployment config.", 
					server.getName()));
		}

		DebuggingContext debugContext = getDebuggingContext(dc);
		int remotePort = debugContext.getDebugPort();
		if( remotePort == -1 ) {
			debugContext.setDebugPort(8787);//TODO get default port from server settings?
		}
		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				if (debuggingContext.getPod() == null) {
					throw toCoreException("Unable to connect to remote pod");
				}
				int localPort = mapPortForwarding(debuggingContext, monitor);

				if(isJavaProject(server)) {
					ILaunch debuggerLaunch = attachRemoteDebugger(server, localPort, monitor);
					if( debuggerLaunch != null ) {
						overrideHotcodeReplace(server, debuggerLaunch);
					}
				}
			}

			@Override
			public void onPodRestart(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				onDebugChange(debuggingContext, monitor);
			}
		};
		debugContext.setDebugListener(listener);
		enableDebugMode(dc, debugContext, monitor);
	}

	/**
	 * Used in
	 * 1) OpenShiftLaunchController.launch() - in a mode other then debug.
	 * 2) OpenShiftShutdownController.stop() - if current mode was debug.
	 *
	 * @param server
	 * @param dc
	 * @param debugContext
	 * @param monitor
	 * @throws CoreException
	 */
	public void stopDebugging(IServer server, IProgressMonitor monitor)
			throws CoreException {
		IDeploymentConfig dc = OpenShiftServerUtils.getDeploymentConfig(server);
		if(dc == null) {
			//something is wrong, do as much as possible.
			terminateRemoteDebugger(server);
			return;
		}
		DebuggingContext debugContext = getDebuggingContext(dc);

		IDebugListener listener = new IDebugListener() {

			@Override
			public void onDebugChange(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
				terminateRemoteDebugger(server);
				unMapPortForwarding(debuggingContext.getPod());
			}

			@Override
			public void onPodRestart(DebuggingContext debuggingContext, IProgressMonitor monitor)
					throws CoreException {
			}
		};
		debugContext.setDebugListener(listener);
		disableDebugMode(dc, debugContext, monitor);
	}

	private ILaunch attachRemoteDebugger(IServer server, int localDebugPort, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Attaching remote debugger");
		ILaunch ret = null;
		ILaunchConfiguration debuggerLaunchConfig = getRemoteDebuggerLaunchConfiguration(server);
		ILaunchConfigurationWorkingCopy workingCopy;
		if (debuggerLaunchConfig == null) {
			workingCopy = createRemoteDebuggerLaunchConfiguration(server);
		} else {
			if (isRunning(debuggerLaunchConfig, localDebugPort)) {
				return null;
			}
			workingCopy = debuggerLaunchConfig.getWorkingCopy();
		}

		IProject project = OpenShiftServerUtils.getDeployProject(server);
		setupRemoteDebuggerLaunchConfiguration(workingCopy, project, localDebugPort);
		debuggerLaunchConfig = workingCopy.doSave();

		long elapsed = 0;
		long increment = 1_000;
		long maxWait =  60_000; //TODO Get from server settings?
		boolean launched = false;
		monitor.subTask("Waiting for remote debug port availability");
		while(!launched && elapsed < maxWait) {
			try {
				//TODO That's fugly. ideally we should see if socket on debug port is responsive instead
				ret = debuggerLaunchConfig.launch(DEBUG_MODE, new NullProgressMonitor());
				launched = true;
			} catch (Exception e) {
				if (monitor.isCanceled()) {
					break;
				}
				try {
					Thread.sleep(increment);
					elapsed+=increment;
				} catch (InterruptedException ie) {
				}
			}
		}

		if (!launched){
			throw toCoreException("Unable to start a remote debugger to localhost:"+localDebugPort);
		}
		
		monitor.worked(10);
		return ret;
	}

	protected boolean overrideHotcodeReplace(IServer server, ILaunch launch) throws CoreException {
		IJavaHotCodeReplaceListener l = getHotCodeReplaceListener(server, launch);
		IDebugTarget[] targets = launch.getDebugTargets();
		if( targets != null && l != null) {
			for( int i = 0; i < targets.length; i++ ) {
				if( targets[i] instanceof IJavaDebugTarget) {
					((IJavaDebugTarget)targets[i]).addHotCodeReplaceListener(l);
				}
			}
		}
		return true;
	}

	protected IJavaHotCodeReplaceListener getHotCodeReplaceListener(IServer server, ILaunch launch) {
		return new ServerHotCodeReplaceListener(server, launch);
	}

	/**
	 * Map the remote port to a local port.
	 * Return the local port in use, or -1 if failed
	 * @param server
	 * @param remotePort
	 * @return the local debug port or -1 if port forwarding did not start or was cancelled.
	 */
	private int mapPortForwarding(final DebuggingContext debuggingContext, final IProgressMonitor monitor) {
		monitor.subTask("Enabling port forwarding");
		IPod pod = debuggingContext.getPod();
		int remotePort = debuggingContext.getDebugPort();
		if (pod == null || remotePort < 1) {
			//nothing we can do
			return -1;
		}
		Set<PortPair> ports = PortForwardingUtils.getForwardablePorts(pod);
		Optional<PortPair> debugPort = ports.stream().filter(p -> remotePort == p.getRemotePort()).findFirst();
		if (!debugPort.isPresent() || monitor.isCanceled()) {
			return -1;
		}

		if (PortForwardingUtils.isPortForwardingStarted(pod)) {
			int p = debugPort.get().getLocalPort();
			return p;
		}

		for (IPortForwardable.PortPair port : ports) {
			if(monitor.isCanceled()) {
				return -1;
			}
			port.setLocalPort(SocketUtil.findFreePort());
			monitor.worked(1);
		}

		PortForwardingUtils.startPortForwarding(pod, ports, OpenShiftBinaryOption.SKIP_TLS_VERIFY);

		if (PortForwardingUtils.isPortForwardingStarted(pod)) {
			int p = debugPort.get().getLocalPort();
			return p;
		}
		//maybe throw exception?
		return -1;
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
				throw toCoreException("Unable to stop port forawrding", e);
			}
		}
	}

	private boolean isJavaProject(IServer server) {
		IProject p = OpenShiftServerUtils.getDeployProject(server);
		try {
			return p != null && p.isAccessible() && p.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			OpenShiftCoreActivator.pluginLog().logError(e);
		}
		return false;
	}

	private CoreException toCoreException(String msg) {
		return toCoreException(msg, null);
	}

	private CoreException toCoreException(String msg, Exception e) {
		return new CoreException(StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, msg, e));
	}
}
