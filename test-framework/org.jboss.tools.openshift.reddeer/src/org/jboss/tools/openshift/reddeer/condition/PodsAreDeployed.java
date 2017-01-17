/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;

/**
 * Wait condition to wait until pods are deployed. That mean desired pod amount is
 * equal to current pod amount in property value of a replication controller.
 * This does not necessary mean that OpenShift explorer view has already been
 * updated and all pods are already visible, it takes a bit longer until those 
 * changes are propagated and visible on this level. Basically it is enough to see updated
 * property value.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class PodsAreDeployed extends AbstractWaitCondition {
	
	private final OpenShiftResource replicationController;
	private final String podAmountValue;
	
	/**
	 * Constructor to wait for a specific amount of pods to be running
	 * @param projectName name of project with a replication controller
	 * @param replicationControllerName name of replication controller
	 * @param desiredAmountOfPods desired amount of running pods
	 */
	public PodsAreDeployed(OpenShiftProject project, String replicationControllerName, int desiredAmountOfPods) {
		assertNotNull(this.replicationController = project.getOpenShiftResource(Resource.DEPLOYMENT, replicationControllerName));
		this.podAmountValue = desiredAmountOfPods + " current / " + desiredAmountOfPods + " desired";
	}
	
	@Override
	public boolean test() {
		replicationController.select();
		return replicationController.getPropertyValue("Misc", "Replicas").trim().equals(
				podAmountValue);
	}

	public static int getNumberOfCurrentReplicas(String project, String replicationControllerName) {
		return getReplicas(getReplicasInfo(project, replicationControllerName));
	}

	public static int getNumberOfCurrentReplicas(OpenShiftProject project, String replicationControllerName) {
		return getReplicas(getReplicasInfo(project, replicationControllerName));
	}

	public static int getNumberOfCurrentReplicas(String server, String username, String project, String replicationControllerName) {
		return getReplicas(getReplicasInfo(server, username, project, replicationControllerName));
	}

	public static String getReplicasInfo(String server, String username, String project, String replicationControllerName) { 
		return getReplicasInfo(new OpenShiftExplorerView().getOpenShift3Connection(server, username).getProject(project), replicationControllerName);
	}

	public static String getReplicasInfo(String project, String replicationControllerName) { 
		return getReplicasInfo(new OpenShiftExplorerView().getOpenShift3Connection().getProject(project), replicationControllerName);
	}

	private static int getReplicas(String replicaInfo) {
		if (StringUtils.isEmpty(replicaInfo)) {
			return -1;
		}

		return Integer.valueOf(replicaInfo.split(" ")[0]);
	}
	
	public static String getReplicasInfo(OpenShiftProject project, String replicationControllerName) { 
		if (project == null
				|| StringUtils.isEmpty(replicationControllerName)) {
			return null;
		}

		OpenShiftResource replicationController = project.getOpenShiftResource(Resource.DEPLOYMENT, replicationControllerName);
		if (replicationController == null) {
			return null;
		}
		return replicationController.getPropertyValue("Misc", "Replicas").trim();
	}
}
