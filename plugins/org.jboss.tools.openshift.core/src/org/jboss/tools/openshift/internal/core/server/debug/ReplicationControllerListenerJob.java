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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;

public class ReplicationControllerListenerJob extends Job {
	/**
	 * For testing purposes
	 */
	public static final String DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY = "deployment.config.listener.job.timeout";

	private Map<String, String> selector = null;
	

	public static final int TIMEOUT = Integer.getInteger(DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY,600_000);//TODO get timeout value from some settings

	private IReplicationController replicationController;
	
	private IPod pod = null;
	
	private Collection<String> oldPods = Collections.emptySet();
	
	private IConnectionsRegistryListener connectionsRegistryListener = new ConnectionsRegistryAdapter() {
		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (pod != null) {
				//we're done already
				return;
			}
			if (selector == null) {
				if (replicationController.equals(oldValue) && newValue instanceof IReplicationController) {
					replicationController = (IReplicationController)newValue;
					selector = replicationController.getReplicaSelector();
				}
				return;
			}
			
			//Wait for new pod once deployment is done
			if (newValue instanceof IPod) {
				IPod candidate = (IPod) newValue;
				String podName = candidate.getName();
				if (!oldPods.contains(podName) &&
					"Running".equals(candidate.getStatus()) &&
					ResourceUtils.containsAll(selector, candidate.getLabels())
						) {
					pod = candidate;
				}
			}
			
		}
	};

	public IConnectionsRegistryListener getConnectionsRegistryListener() {
		return connectionsRegistryListener;
	}

	public ReplicationControllerListenerJob(IReplicationController replicationController) {
		super("Waiting for OpenShift Pod redeployment");
		this.replicationController = replicationController;
		oldPods = ResourceUtils.getPodsFor(replicationController).stream().map(p -> p.getName()).collect(Collectors.toList());
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		long elapsed = 0;
		int wait = 100;
		while(pod == null && !monitor.isCanceled() && elapsed < TIMEOUT) {
			try {
				Thread.sleep(wait);
				monitor.worked(1);
				elapsed += wait;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return pod==null? getTimeOutStatus() : Status.OK_STATUS;
	}

	public IStatus getTimeOutStatus() {
		return new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, "Failed to detect new deployed Pod for "+replicationController.getName());	
	}
	
	public IPod getPod() {
		return pod;
	}
}
