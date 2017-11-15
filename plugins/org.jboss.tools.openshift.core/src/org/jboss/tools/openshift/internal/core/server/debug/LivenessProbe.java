/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.probe.IProbe;

/**
 * A class that allows to manipulate an existing liveness probe for a given deployment config.
 * 
 * @author Andre Dietisheim
 * 
 * @see <a href="https://kubernetes.io/docs/api-reference/v1.8/#probe-v1-core"/>
 */
public class LivenessProbe {

	public static final int INITIAL_DELAY = 1 * 60 * 60; // 1h
	
	private IDeploymentConfig dc;

	LivenessProbe(IDeploymentConfig dc) {
		this.dc = dc;
	}
	
	/**
	 * Sets the initial delay of the liveness probe for the given deployment config.
	 * No action is taken if there's no such probe. Returns {@code true} if the
	 * probe was modified, {@code false} otherwise.
	 * 
	 * @param delay
	 * @param monitor
	 * @return
	 * @throws CoreException 
	 */
	public boolean setInitialDelay(DebugContext context, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(NLS.bind("Setting liveness probe initial delay to {0}", INITIAL_DELAY));

		IProbe probe = getLivenessProbe(dc);
		if (probe == null) {
			return false;
		}

		int initialDelay = probe.getInitialDelaySeconds();
		if (initialDelay >= INITIAL_DELAY) {
			return false;
		}
		OpenShiftServerUtils.setLivenessProbeInitialDelay(initialDelay, context.getServer());
		probe.setInitialDelaySeconds(INITIAL_DELAY);
		return true;
	}

	/**
	 * Restores the initial initial timeout in liveness probe of the given dc or removes it if there was no value before we set it.
	 * 
	 * @param context
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public boolean resetInitialDelay(DebugContext context, IProgressMonitor monitor) throws CoreException {
		int delay = OpenShiftServerUtils.getLivenessProbeInitialDelay(context.getServer());
		if (delay == OpenShiftServerUtils.VALUE_LIVENESSPROBE_NODELAY) {
			return false;
		}

		IProbe probe = getLivenessProbe(dc);
		if (probe == null) {
			return false;
		}
		
		monitor.subTask("Resetting liveness probe initial delay...");
		probe.setInitialDelaySeconds(delay);
		OpenShiftServerUtils.setLivenessProbeInitialDelay(OpenShiftServerUtils.VALUE_LIVENESSPROBE_NODELAY, context.getServer());
		return true;
	}

	private IProbe getLivenessProbe(IDeploymentConfig dc) {
		Collection<IContainer> containers = dc.getContainers();
		if (null == containers) {
			return null;
		}
		IContainer container = containers.stream().findFirst().orElse(null);
		if (container == null) {
			return null;
		}

		return container.getLivenessProbe();
	}
}
