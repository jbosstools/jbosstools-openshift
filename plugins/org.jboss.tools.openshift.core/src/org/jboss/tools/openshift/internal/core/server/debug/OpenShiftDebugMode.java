/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.util.NewPodDetectorJob;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;

/**
 * A class that handles the debug and devmode mode in OpenShift.
 * 
 * @author Fred Bricon
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class OpenShiftDebugMode {
	
	private DebugContext context;

	/**
	 * For testing purposes
	 */
	public OpenShiftDebugMode(DebugContext context) {
		Assert.isNotNull(context);
		this.context = context;
	}

	/**
	 * Enables debugging in given context.  No change is
	 * executed in OpenShift, the change is only in the given local context. To have
	 * the changes set to OpenShift one has to call
	 * {@link #sendChanges(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 * @param monitor
	 */
	public OpenShiftDebugMode enableDebugging() {
		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);

		return this;
	}

	/**
	 * Disables debugging and the devmode in the given context. No change is
	 * executed in OpenShift, the change is only in the given local context. To have
	 * the changes set to OpenShift one has to call
	 * {@link #sendChanges(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 * 
	 * @see {@link #sendChanges(DebugContext, IProgressMonitor)}
	 */
	public OpenShiftDebugMode disableDebugging() {
		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);

		return this;
	}

	private void updateDebugEnvVariables(IDeploymentConfig dc, DebugContext context) {
		if (context.isDebugEnabled()) {
			setDebugPort(context.getDebugPort(), dc);
			dc.setEnvironmentVariable(context.getDebugPortKey(), String.valueOf(context.getDebugPort()));
			dc.setEnvironmentVariable(context.getDevmodeKey(), String.valueOf(context.isDebugEnabled()));
		} else {
			dc.removeEnvironmentVariable(context.getDebugPortKey());
			dc.removeEnvironmentVariable(context.getDevmodeKey());
		}
	}

	private void setDebugPort(int debugPort, IDeploymentConfig dc) {
		Collection<IContainer> originalContainers = dc.getContainers();
		if (originalContainers != null 
				&& !originalContainers.isEmpty()) {
			Collection<IContainer> containers = new ArrayList<>(originalContainers);
			IContainer container = containers.iterator().next();
			Set<IPort> ports = new HashSet<>(container.getPorts());
			
			IPort existing = ports.stream()
					.filter(p -> p.getContainerPort() == debugPort)
					.findFirst()
					.orElse(null);
			boolean added = addDebugPort(debugPort, ports, existing);
			if (added) {
				container.setPorts(ports); 
				dc.setContainers(containers);
			}
		}
	}

	private boolean addDebugPort(int debugPort, Set<IPort> ports, IPort existing) {
		boolean added = false;
		if (existing == null) {
			PortSpecAdapter newPort = new PortSpecAdapter("debug", "TCP", debugPort);
			added = ports.add(newPort);
		} else {
			PortSpecAdapter newPort = new PortSpecAdapter(existing.getName(), existing.getProtocol(), debugPort);
			if (!existing.equals(newPort)) {
				ports.remove(existing);
				added = ports.add(newPort);
			}
		}
		return added;
	}

	
	private boolean isDebugEnabled(IDeploymentConfig dc, String devmodeKey, String debugPortKey, int requestedDebugPort, boolean enableRequested) {
		boolean debugEnabled = false;
		boolean devmodeEnabled = isDevmodeEnabled(dc, devmodeKey);
		if (devmodeEnabled) {
			// debugging is enabled if devmode and debug port are set
			String debugPort = getEnv(dc, debugPortKey);
			if (enableRequested) {
				// if we should enable, compare current port to requested one
				debugEnabled = !StringUtils.isBlank(debugPort) 
						&& context.getDebugPort(debugPort) == requestedDebugPort;
			} else {
				// if we should disable, simply check if debug port exists
				debugEnabled = !StringUtils.isBlank(debugPort);
			}
		}
		return debugEnabled;
	}

	/**
	 * Enables devmode in given context.  No change is
	 * executed in OpenShift, the change is only in the given local context. To have
	 * the changes set to OpenShift one has to call
	 * {@link #sendChanges(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 */
	public OpenShiftDebugMode enableDevmode() {
		context.setDevmodeEnabled(true);
		
		return this;
	}
	
	/**
	 * Disables devmode in given context.  No change is
	 * executed in OpenShift, the change is only in the given local context. To have
	 * the changes set to OpenShift one has to call
	 * {@link #sendChanges(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 */
	public OpenShiftDebugMode disableDevmode() {
		context.setDevmodeEnabled(false);

		return this;
	}

	/**
	 * Sends the changes to the deployment config if required. Nothing is done if
	 * the deployment config already is in the state that the context is in. If the
	 * changes are sent the existing pods are killed and it waits until the new pods
	 * are running.
	 * 
	 * @param context
	 * @param monitor
	 * @throws CoreException
	 */
	public OpenShiftDebugMode execute(IProgressMonitor monitor) throws CoreException {
		IDeploymentConfig dc = getDeploymentConfig(context, monitor);
		if (dc == null) {
			throw new CoreException(StatusFactory.errorStatus(
					OpenShiftCoreActivator.PLUGIN_ID, "No deployment config present that can be updated."));
		}

		if (updateDebugmode(dc, context, monitor)
				| updateDevmode(dc, context, monitor)) {
			sendUpdated(dc, context, monitor);
		}

		if (context.isDebugEnabled()) {
			IDebugListener listener = context.getDebugListener();
			if (listener != null) {
				listener.onDebugChange(context, monitor);
			}
		}
		
		return this;
	}

	private boolean updateDebugmode(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(context.isDebugEnabled()? "Enabling" : "Disabling" 
			+ " debugging for deployment config {0}", dc.getName()));

		boolean needsUpdate = needsDebugUpdate(dc, context);
		if (needsUpdate) {
			updateDebugEnvVariables(dc, context);
		}
		return needsUpdate;
	}

	private boolean needsDebugUpdate(IDeploymentConfig dc, DebugContext context) {
		boolean debugEnabled = isDebugEnabled(dc, 
				context.getDevmodeKey(), context.getDebugPortKey(), context.getDebugPort(), context.isDebugEnabled());
		return debugEnabled != context.isDebugEnabled();
	}

	private boolean updateDevmode(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind("Enabling devmode for deployment config {0}", dc.getName()));
		boolean needsUpdate = needsDevmodeUpdate(dc, context);
		if (needsUpdate) {
			updateDevmodeEnvVar(context.isDevmodeEnabled(), dc, context);
		}
		return needsUpdate;
	}

	private boolean needsDevmodeUpdate(IDeploymentConfig dc, DebugContext context) {
		boolean devmodeEnabled = isDevmodeEnabled(dc, context.getDevmodeKey());
		return devmodeEnabled != context.isDevmodeEnabled();
	}

	private boolean isDevmodeEnabled(IDeploymentConfig dc, String devmodeKey) {
		return Boolean.parseBoolean(getEnv(dc, devmodeKey));
	}

	private String getEnv(IDeploymentConfig dc, String key) {
		if (dc == null
				|| dc.getEnvironmentVariables() == null
				|| StringUtils.isEmpty(key)) {
			return null;
		}
		Optional<IEnvironmentVariable> envVar = dc.getEnvironmentVariables().stream()
				.filter(ev -> key.equals(ev.getName()))
				.findFirst();
		if (envVar.isPresent()) {
			return envVar.get().getValue();
		}
		return null;
	}

	private void updateDevmodeEnvVar(boolean enable, IDeploymentConfig dc, DebugContext context) {
		if (enable) {
			dc.setEnvironmentVariable(context.getDevmodeKey(), String.valueOf(enable));
		} else {
			dc.removeEnvironmentVariable(context.getDevmodeKey());
		}
	}

	protected void sendUpdated(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Updating replication controller {0} and waiting for new pods to run.", dc.getName()));

		Connection connection = ConnectionsRegistryUtil.getConnectionFor(dc);
		connection.updateResource(dc);
		// do not kill all existing pods, the rc will re-create new ones before the
		// updated dc eventually then kills them and re-creates new ones.
		IPod pod = waitForNewPod(dc, monitor);
		context.setPod(pod);
	}
	
	private IDeploymentConfig getDeploymentConfig(DebugContext context, IProgressMonitor monitor) throws CoreException {
		return OpenShiftServerUtils.getDeploymentConfig(context.getServer(), monitor);
	}

	private IPod waitForNewPod(IDeploymentConfig dc, IProgressMonitor monitor) throws CoreException {
		NewPodDetectorJob newPodDetector = new NewPodDetectorJob(dc);
		newPodDetector.schedule();
		return waitFor(newPodDetector, monitor);
	}

	private IPod waitFor(NewPodDetectorJob podDetector, IProgressMonitor monitor) throws CoreException {
		try {
			podDetector.join(NewPodDetectorJob.TIMEOUT, monitor);
			IStatus result = podDetector.getResult();
			if (result == null) {//timed out!
				throw new CoreException(podDetector.getTimeOutStatus());
			} else if (!result.isOK()) {
				throw new CoreException(result);
			}
			return podDetector.getPod();
		} catch (OperationCanceledException | InterruptedException e) {
			throw new OpenShiftCoreException(e);
		}
	}


}
