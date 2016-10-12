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
import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE;

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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.IClient;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

public class OpenShiftDebugUtils {

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
			updateDebugConfig(deploymentConfig, debugContext, monitor);
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
			updateDebugConfig(deploymentConfig, debugContext, monitor);
		}
		return debugContext;
	}
	
	public void updateDebugConfig(IDeploymentConfig deploymentConfig, DebuggingContext debugContext, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Updating Deployment Configuration");
		if (deploymentConfig == null
				|| deploymentConfig.getEnvironmentVariables() == null) {
			return;
		}
		updateDeploymentConfigValues(deploymentConfig, debugContext);
		
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

	private void updateDeploymentConfigValues(IDeploymentConfig deploymentConfig,
			DebuggingContext debugContext) {
		Collection<IContainer> originalContainers = deploymentConfig.getContainers();
		if (originalContainers != null && !originalContainers.isEmpty() && debugContext.isDebugEnabled()) {
			Collection<IContainer> containers = new ArrayList<>(originalContainers);
			IContainer container = containers.iterator().next();
			Set<IPort> ports = new HashSet<>(container.getPorts());
			
			IPort existing = ports.stream().filter(p -> p.getContainerPort() == debugContext.getDebugPort())
										.findFirst().orElse(null);
			boolean added = false;
			if (existing == null) {
				PortSpecAdapter newPort = new PortSpecAdapter("debug", "TCP", debugContext.getDebugPort());
				added = ports.add(newPort);
			} else {
				PortSpecAdapter newPort = new PortSpecAdapter(existing.getName(), existing.getProtocol(), debugContext.getDebugPort());
				if (!existing.equals(newPort)) {
					ports.remove(existing);
					added = ports.add(newPort);
				}
			}
			if (added) {
				container.setPorts(ports); 
				deploymentConfig.setContainers(containers);
			}
		}
		//TODO the list of env var to set in debug mode should probably be defined in the server settings instead
		deploymentConfig.setEnvironmentVariable(DEBUG_PORT_KEY, String.valueOf(debugContext.getDebugPort()));
		deploymentConfig.setEnvironmentVariable(DEV_MODE_KEY, String.valueOf(debugContext.isDebugEnabled()));//for node
		deploymentConfig.setEnvironmentVariable(DEBUG_KEY, String.valueOf(debugContext.isDebugEnabled()));//for eap
	}

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
		workingCopy.setAttribute(ATTR_ALLOW_TERMINATE, true);
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
}
