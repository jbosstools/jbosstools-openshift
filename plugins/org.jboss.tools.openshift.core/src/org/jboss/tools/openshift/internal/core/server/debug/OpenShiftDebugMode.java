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
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
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

	public static final String DEFAULT_DEVMODE_KEY = "DEV_MODE";
	public static final String DEFAULT_DEBUG_PORT_KEY = "DEBUG_PORT";
	public static final String DEFAULT_DEBUG_PORT = "8787";
	
	private OpenShiftDebugMode() {
	}

	/**
	 * Creates a debugging context instance for the given server behaviour with the
	 * given environment keys/values that should be used in the OpenShift deployment
	 * config.
	 * 
	 * @param behaviour the server behaviour that will be used for this context.
	 * @param devmodeKey
	 *            the env key to use to get/set devmode in the deployment config
	 * @param debugPortKey
	 *            the env key to use to get/set the debug port in the deployment
	 *            config
	 * @param debugPort
	 *            the debug port to use in the deployment config
	 * @return
	 */
	public static DebugContext createContext(IControllableServerBehavior behaviour, String devmodeKey, String debugPortKey, String debugPort) {
		Assert.isNotNull(behaviour);
		
		DebugContext context = new DebugContext(behaviour, 
				StringUtils.defaultIfBlank(devmodeKey, DEFAULT_DEVMODE_KEY),
				StringUtils.defaultIfBlank(debugPortKey, DEFAULT_DEBUG_PORT_KEY),
				StringUtils.defaultIfBlank(debugPort, DEFAULT_DEBUG_PORT));

		return context;
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
	public static void enableDebugging(DebugContext context) {
		Assert.isNotNull(context);

		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);
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
	public static void disableDebugging(DebugContext context) {
		Assert.isNotNull(context);

		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);
	}

	private static void updateDebugEnvVariables(IDeploymentConfig dc, DebugContext context) {
		if (context.isDebugEnabled()) {
			setDebugPort(context.getDebugPort(), dc);
			dc.setEnvironmentVariable(context.getDebugPortKey(), String.valueOf(context.getDebugPort()));
			dc.setEnvironmentVariable(context.getDevmodeKey(), String.valueOf(context.isDebugEnabled()));
		} else {
			dc.removeEnvironmentVariable(context.getDebugPortKey());
			dc.removeEnvironmentVariable(context.getDevmodeKey());
		}
	}

	private static void setDebugPort(int debugPort, IDeploymentConfig dc) {
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

	private static boolean addDebugPort(int debugPort, Set<IPort> ports, IPort existing) {
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

	private static boolean isDebugEnabled(IDeploymentConfig dc, String devmodeKey, String debugPortKey) {
		boolean debugEnabled = false;
		boolean devmodeEnabled = isDevmodeEnabled(dc, devmodeKey);
		if (devmodeEnabled) {
			// debugging is enabled if devmode and debug port are set
			String debugPort = getEnv(dc, debugPortKey);
			debugEnabled = !StringUtils.isBlank(debugPort);
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
	public static void enableDevmode(DebugContext context) {
		context.setDevmodeEnabled(true);
	}
	
	/**
	 * Disables devmode in given context.  No change is
	 * executed in OpenShift, the change is only in the given local context. To have
	 * the changes set to OpenShift one has to call
	 * {@link #sendChanges(DebugContext, IProgressMonitor)}
	 * 
	 * @param context
	 */
	public static void disableDevmode(DebugContext context) {
		context.setDevmodeEnabled(false);
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
	public static void sendChanges(DebugContext context, IProgressMonitor monitor) throws CoreException {
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
	}

	private static boolean updateDebugmode(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(context.isDebugEnabled()? "Enabling" : "Disabling" 
			+ " debugging for deployment config {0}", dc.getName()));

		boolean needsUpdate = needsDebugUpdate(dc, context);
		if (needsUpdate) {
			updateDebugEnvVariables(dc, context);
		}
		return needsUpdate;
	}

	private static boolean needsDebugUpdate(IDeploymentConfig dc, DebugContext context) {
		boolean debugEnabled = isDebugEnabled(dc, context.getDevmodeKey(), context.getDebugPortKey());
		return debugEnabled != context.isDebugEnabled();
	}

	private static boolean updateDevmode(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind("Enabling devmode for deployment config {0}", dc.getName()));
		boolean needsUpdate = needsDevmodeUpdate(dc, context);
		if (!needsUpdate) {
			return false;
		}

		updateDevmodeEnvVar(context.isDevmodeEnabled(), dc, context);
		return true;
	}

	private static boolean needsDevmodeUpdate(IDeploymentConfig dc, DebugContext context) {
		boolean devmodeEnabled = isDevmodeEnabled(dc, context.getDevmodeKey());
		return devmodeEnabled != context.isDevmodeEnabled();
	}

	private static boolean isDevmodeEnabled(IDeploymentConfig dc, String devmodeKey) {
		return Boolean.parseBoolean(getEnv(dc, devmodeKey));
	}

	private static String getEnv(IDeploymentConfig dc, String key) {
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

	private static void updateDevmodeEnvVar(boolean enable, IDeploymentConfig dc, DebugContext context) {
		if (enable) {
			dc.setEnvironmentVariable(context.getDevmodeKey(), String.valueOf(enable));
		} else {
			dc.removeEnvironmentVariable(context.getDevmodeKey());
		}
	}

	private static void sendUpdated(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Updating replication controller {0} and waiting for new pods to run.", dc.getName()));

		Connection connection = ConnectionsRegistryUtil.getConnectionFor(dc);
		connection.updateResource(dc);
		// do not kill all existing pods, the rc will re-create new ones before the
		// updated dc eventually then kills that one and re-creates a new one.
		IPod pod = waitForNewPod(dc, monitor);
		context.setPod(pod);
	}
	
	private static IDeploymentConfig getDeploymentConfig(DebugContext context, IProgressMonitor monitor) throws CoreException {
		return OpenShiftServerUtils.getDeploymentConfig(context.getServerBehaviour().getServer(), monitor);
	}

	private static IPod waitForNewPod(IDeploymentConfig dc, IProgressMonitor monitor) throws CoreException {
		NewPodDetectorJob newPodDetector = new NewPodDetectorJob(dc);
		newPodDetector.schedule();
		return waitFor(newPodDetector, monitor);
	}

	private static IPod waitFor(NewPodDetectorJob podDetector, IProgressMonitor monitor) throws CoreException {
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

	public static class DebugContext {
		
		public static final int NO_DEBUG_PORT = -1;
		
		private IControllableServerBehavior behaviour;
		
		private boolean debugEnabled;
		private boolean devmodeEnabled;
		private String devmodeKey;
		private String debugPortKey;
		private int debugPort = NO_DEBUG_PORT;
		private IDebugListener listener;
		private IPod pod;

		private DebugContext(IControllableServerBehavior behaviour, String devmodeKey, String debugPortKey, String debugPort) {
			this.behaviour = behaviour;
			this.devmodeKey = devmodeKey;
			this.debugPortKey = debugPortKey;
			this.debugPort = getDebugPort(debugPort);
		}

		public IControllableServerBehavior getServerBehaviour() {
			return behaviour;
		}

		private void setDebugEnabled(boolean debugEnabled) {
			this.debugEnabled = debugEnabled;
		}

		public boolean isDebugEnabled() {
			return debugEnabled;
		}

		public void setDevmodeEnabled(boolean devmodeEnabled) {
			this.devmodeEnabled = devmodeEnabled;
		}

		public boolean isDevmodeEnabled() {
			return devmodeEnabled;
		}

		public int getDebugPort() {
			return debugPort;
		}

		private int getDebugPort(String debugPort) {
			if (StringUtils.isBlank(debugPort)) {
				return NO_DEBUG_PORT;
			}
			
			try {
				return Integer.parseInt(debugPort);
			} catch(NumberFormatException e) {
				return NO_DEBUG_PORT;
			}
		}

		public IDebugListener getDebugListener() {
			return listener;
		}

		public void setDebugListener(IDebugListener listener) {
			this.listener = listener;
		}

		private String getDevmodeKey() {
			return devmodeKey;
		}

		private String getDebugPortKey() {
			return debugPortKey;
		}

		private void setPod(IPod pod) {
			this.pod = pod;
		}

		public IPod getPod() {
			return this.pod;
		}

	}
}
