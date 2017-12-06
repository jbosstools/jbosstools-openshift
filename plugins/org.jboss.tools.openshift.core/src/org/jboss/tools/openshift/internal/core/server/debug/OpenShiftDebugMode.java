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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
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
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.util.NewPodDetectorJob;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.route.IRoute;

/**
 * A class that handles the debug and devmode mode in OpenShift.
 * 
 * @author Fred Bricon
 * @author Jeff Maury
 * @author Andre Dietisheim
 */
public class OpenShiftDebugMode {

	private static final String DEBUG_PORT_PROTOCOL = "TCP";
	private static final String DEBUG_PORT_NAME = "debug";

	protected DebugContext context;

	public OpenShiftDebugMode(DebugContext context) {
		Assert.isNotNull(context);
		this.context = context;
	}

	/**
	 * Enables debugging in given context. Debugging implies devmode being enabled,
	 * too. 
	 * No change is executed in OpenShift, the change is only in the given
	 * local context. To have the changes set to OpenShift one has to call
	 * {@link #send(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 * @param monitor
	 * 
	 * @see #send(IDeploymentConfig, Connection, IProgressMonitor)
	 * @see #enableDevmode()
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

	private boolean isDebugEnabled(IDeploymentConfig dc, String devmodeKey, String debugPortKey, int requestedDebugPort,
			boolean enableRequested) {
		boolean debugEnabled = false;
		EnvironmentVariables env = new EnvironmentVariables(dc);
		boolean devmodeEnabled = env.getBoolean(devmodeKey);
		if (devmodeEnabled) {
			// debugging is enabled if devmode and debug port are set
			String debugPort = env.getString(debugPortKey);
			if (enableRequested) {
				// if we should enable, compare current port to requested one
				debugEnabled = !StringUtils.isBlank(debugPort) && context.getDebugPort(debugPort) == requestedDebugPort;
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
		Connection connection = OpenShiftServerUtils.getConnection(context.getServer());
		IResource resource = OpenShiftServerUtils.getResource(context.getServer(), monitor);
		IDeploymentConfig dc = getDeploymentConfig(resource, connection, monitor);
		if (dc == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind(
							"Could not find deployment config for resource {0}. "
									+ "Your build might be still running and pods not created yet or "
									+ "there might be no labels on your pods pointing to the wanted deployment config.",
							resource != null ? resource.getName() : "")));
		}

		boolean dcUpdated = updateDc(dc, connection, monitor);
		IPod pod = getPod(dcUpdated, dc, connection, monitor);
		context.setPod(pod);

		toggleRouteTimeout(resource, connection, context, monitor);
		toggleDebugger(context, monitor);

		return this;
	}

	private void toggleRouteTimeout(IResource resource, Connection connection, DebugContext context,
			IProgressMonitor monitor) throws CoreException {
		if (context.isDebugEnabled()) {
			setRouteTimeout(resource, connection, monitor);
		} else {
			resetRouteTimeout(resource, connection, monitor);
		}
	}

	private void setRouteTimeout(IResource resource, Connection connection, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask("Increasing route timeout while debugging...");

		IRoute route = new RouteTimeout(resource, connection).set(context, monitor);
		if (route != null) {
			safeSend(route, connection, monitor);
		}
	}

	private void resetRouteTimeout(IResource resource, Connection connection, IProgressMonitor monitor)
			throws CoreException {
		monitor.subTask("Clearing/restoring route timeout after debugging...");

		IRoute route = new RouteTimeout(resource, connection).reset(context, monitor);
		if (route != null) {
			safeSend(route, connection, monitor);
		}
	}

	private boolean updateDc(IDeploymentConfig dc, Connection connection, IProgressMonitor monitor)
			throws CoreException {
		boolean dcUpdated = updateDebugmode(dc, context, monitor) | updateDevmode(dc, context, monitor)
				| updateLifenessProbe(dc, context, monitor);

		if (dcUpdated) {
			send(dc, connection, monitor);
		}

		return dcUpdated;
	}

	protected IPod getPod(boolean dcUpdated, IDeploymentConfig dc, Connection connection, IProgressMonitor monitor)
			throws CoreException {
		IPod pod = null;
		if (dcUpdated) {
			// do not kill all existing pods, the rc will re-create new ones before the
			// updated dc eventually then kills them and re-creates new ones.
			pod = waitForNewPod(dc, monitor);
		} else {
			// dc already correctly set, so just get the pod
			pod = getExistingPod(dc, connection, monitor);
		}
		return pod;
	}

	private void toggleDebugger(DebugContext context, IProgressMonitor monitor) throws CoreException {
		if (context.isDebugEnabled()) {
			IDebugListener listener = context.getDebugListener();
			if (listener != null) {
				listener.onDebugChange(context, monitor);
			}
		}
	}

	protected IPod getExistingPod(IDeploymentConfig dc, Connection connection, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind("Retrieving existing pod for deployment config {0}.", dc.getName()));

		List<IPod> allPods = connection.getResources(ResourceKind.POD, dc.getNamespace());
		// TODO: support multiple pods
		return ResourceUtils.getPodsFor(dc, allPods).stream().findFirst().orElse(null);

	}

	private boolean updateDebugmode(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) {
		monitor.subTask(
				NLS.bind(context.isDebugEnabled() ? "Enabling" : "Disabling" + " debugging for deployment config {0}",
						dc.getName()));

		boolean needsUpdate = needsDebugUpdate(dc, context);
		if (needsUpdate) {
			updateContainerDebugPort(dc, context);
			updateDebugEnvVariables(dc, context);
		}

		return needsUpdate;
	}

	private boolean needsDebugUpdate(IDeploymentConfig dc, DebugContext context) {
		boolean debugEnabled = isDebugEnabled(dc, context.getDevmodeKey(), context.getDebugPortKey(),
				context.getDebugPort(), context.isDebugEnabled());
		return debugEnabled != context.isDebugEnabled();
	}

	private void updateDebugEnvVariables(IDeploymentConfig dc, DebugContext context) {
		if (context.isDebugEnabled()) {
			dc.setEnvironmentVariable(context.getDebugPortKey(), String.valueOf(context.getDebugPort()));
			dc.setEnvironmentVariable(context.getDevmodeKey(), String.valueOf(context.isDebugEnabled()));
		} else {
			dc.removeEnvironmentVariable(context.getDebugPortKey());
			dc.removeEnvironmentVariable(context.getDevmodeKey());
		}
	}

	private void updateContainerDebugPort(IDeploymentConfig dc, DebugContext context) {
		Collection<IContainer> containers = dc.getContainers();
		if (CollectionUtils.isEmpty(containers)) {
			return;
		}

		// TODO: support multiple containers
		IContainer firstContainer = containers.iterator().next();
		IPort currentContainerPort = getCurrentContainerPort(firstContainer.getPorts());

		boolean modified = false;
		Set<IPort> ports = new HashSet<>(firstContainer.getPorts());
		if (context.isDebugEnabled()) {
			modified = addReplaceDebugPort(context.getDebugPort(), currentContainerPort, ports);
		} else {
			modified = removeDebugPort(currentContainerPort, ports);
		}

		if (modified) {
			firstContainer.setPorts(ports);
		}
	}

	private IPort getCurrentContainerPort(Set<IPort> ports) {
		return ports.stream().filter(p ->
		// find by name
		DEBUG_PORT_NAME.equals(p.getName())).findFirst().orElse(null);
	}

	private boolean addReplaceDebugPort(int debugPort, IPort existing, Set<IPort> ports) {
		boolean modified = false;
		if (matchesPort(debugPort, existing)) {
			ports.remove(existing);
			modified = true;
		}
		PortSpecAdapter newPort = new PortSpecAdapter(DEBUG_PORT_NAME, DEBUG_PORT_PROTOCOL, debugPort);
		// use bit-wise OR to avoid short-circuit
		modified = modified | ports.add(newPort);
		return modified;
	}

	private boolean matchesPort(int debugPort, IPort currentContainerPort) {
		return currentContainerPort != null && currentContainerPort.getContainerPort() != debugPort;
	}

	private boolean removeDebugPort(IPort currentContainerPort, Set<IPort> ports) {
		boolean modified = false;
		if (currentContainerPort != null) {
			ports.remove(currentContainerPort);
			modified = true;
		}
		return modified;
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
		boolean devmodeEnabled = new EnvironmentVariables(dc).getBoolean(context.getDevmodeKey());
		return devmodeEnabled != context.isDevmodeEnabled();
	}

	private void updateDevmodeEnvVar(boolean enable, IDeploymentConfig dc, DebugContext context) {
		if (enable) {
			new EnvironmentVariables(dc).set(context.getDevmodeKey(), String.valueOf(enable));
		} else {
			new EnvironmentVariables(dc).remove(context.getDevmodeKey());
		}
	}

	private boolean updateLifenessProbe(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor)
			throws CoreException {
		if (context.isDebugEnabled()) {
			return new LivenessProbe(dc).setInitialDelay(context, monitor);
		} else {
			return new LivenessProbe(dc).resetInitialDelay(context, monitor);
		}
	}

	protected void safeSend(IResource resource, Connection connection, IProgressMonitor monitor) {
		try {
			send(resource, connection, monitor);
		} catch (CoreException e) {
			OpenShiftCoreActivator.pluginLog().logError(e.getMessage());
		}
	}

	protected void send(IResource resource, Connection connection, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Updating {0}...", resource.getName()));

		try {
			connection.updateResource(resource);
		} catch (OpenShiftException e) {
			throw new CoreException(
					StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
							NLS.bind("Could not update resource {0}.", resource.getName()), e));
		}
	}

	private IDeploymentConfig getDeploymentConfig(IResource resource, Connection connection, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind("Retrieving deployment config for resource {0}.", resource.getName()));

		return ResourceUtils.getDeploymentConfigFor(resource, connection);
	}

	protected IPod waitForNewPod(IDeploymentConfig dc, IProgressMonitor monitor) throws CoreException {
		NewPodDetectorJob newPodDetector = new NewPodDetectorJob(dc);
		newPodDetector.schedule();
		return waitFor(newPodDetector, monitor);
	}

	protected IPod waitFor(NewPodDetectorJob podDetector, IProgressMonitor monitor) throws CoreException {
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
