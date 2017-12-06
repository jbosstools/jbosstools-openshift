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
package org.jboss.tools.openshift.internal.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;

/**
 * A job that waits for new pods for a given, updated replication controller to reappear.
 *  
 * @author Fred Bricon
 * @author Andre Dietisheim
 */
public class NewPodDetectorJob extends Job {

	/**
	 * For testing purposes
	 */
	public static final String DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY = "deployment.config.listener.job.timeout";
	//TODO get timeout value from some settings
	public static final int TIMEOUT = Integer.getInteger(DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, 600_000);
	private static final int SLEEP_DELAY = 100;
	private static final String POD_STATE_RUNNING = "Running";

	private IDeploymentConfig dc;
	private IPod pod;
	private Collection<String> oldPods = Collections.emptySet();

	private IConnectionsRegistryListener connectionsRegistryListener = new ConnectionsRegistryAdapter() {
		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (pod != null) {
				// we're done already
				return;
			}

			if (newValue instanceof IDeploymentConfig && !dc.equals(oldValue)) {
				dc = (IDeploymentConfig) newValue;
				return;
			}

			if (newValue instanceof IPod) {
				IPod notifiedPod = (IPod) newValue;
				if (isNewRunningRuntimePod(notifiedPod)) {
					// store new & running runtime pod for job to stop waiting
					pod = notifiedPod;
				}
			}
		}

		private boolean isNewRunningRuntimePod(IPod pod) {
			return ResourceUtils.isRuntimePod(pod) && !oldPods.contains(pod.getName())
					&& POD_STATE_RUNNING.equals(pod.getStatus()) && ResourceUtils.areRelated(pod, dc);
		}
	};

	public NewPodDetectorJob(IDeploymentConfig dc) {
		super("Waiting for OpenShift Pod redeployment");
		this.dc = dc;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		this.oldPods = getOldPods(dc);
		try {
			ConnectionsRegistrySingleton.getInstance().addListener(connectionsRegistryListener);
			waitForNewPod(monitor);
			return pod == null ? getTimeOutStatus() : Status.OK_STATUS;
		} finally {
			ConnectionsRegistrySingleton.getInstance().removeListener(connectionsRegistryListener);
		}
	}

	private Collection<String> getOldPods(IReplicationController rc) {
		Connection connection = ConnectionsRegistryUtil.getConnectionFor(rc);
		List<IPod> allPods = connection.getResources(ResourceKind.POD, rc.getNamespace());
		return ResourceUtils.getPodsFor(rc, allPods).stream().filter(pod -> ResourceUtils.isRuntimePod(pod))
				.map(p -> p.getName()).collect(Collectors.toList());
	}

	private void waitForNewPod(IProgressMonitor monitor) {
		long elapsed = 0;
		while (pod == null && !monitor.isCanceled() && elapsed < TIMEOUT) {
			try {
				Thread.sleep(SLEEP_DELAY);
				monitor.worked(1);
				elapsed += SLEEP_DELAY;
			} catch (InterruptedException e) {
				// swallow intentionally
			}
		}
	}

	public IStatus getTimeOutStatus() {
		return new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID,
				"Failed to detect new deployed Pod for " + dc.getName());
	}

	public IPod getPod() {
		return pod;
	}
}
