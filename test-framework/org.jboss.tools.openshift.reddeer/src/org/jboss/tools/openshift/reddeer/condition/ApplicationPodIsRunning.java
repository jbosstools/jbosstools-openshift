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

import java.util.List;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;

/**
 * Wait condition to wait till application pod is up and running.
 * Once condition passes it is possible to get a name of running pod
 * via getApplicationPodName method.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ApplicationPodIsRunning extends AbstractWaitCondition {

	private OpenShiftProject project;
	
	private String applicationPodName;
	
	/**
	 * Constructs a new ApplicationPodsRunning wait condition.
	 * Pods containing 'build' and 'deploy' key words in their name
	 * are excluded from list of suitable applications pods. Be aware
	 * that if there is a pod having one of that key words condition is 
	 * not met. 
	 */
	public ApplicationPodIsRunning() {
		OpenShiftExplorerView explorer  = new OpenShiftExplorerView();
		explorer.open();
		this.project = explorer.getOpenShift3Connection().getProject();
	}

	public ApplicationPodIsRunning(OpenShiftProject project) {
		this.project = project;
	}

	@Override
	public boolean test() {
		project.refresh();
		List<OpenShiftResource> pods = project.getOpenShiftResources(Resource.POD);
	
		if (pods.isEmpty()) {
			return false;
		}
		
		// TODO: this FLAWED: it assumes that all pods that it finds within a
		// project are the pods for the application that a test wants to wait
		// for
		for (OpenShiftResource resource: pods) {
			if (!resource.getName().contains("build") && 
					!resource.getName().contains("deploy")) {
				if (resource.getStatus().equals(ResourceState.RUNNING.toString())) {
					applicationPodName = resource.getName();
					return true;
				}
			}
		}
		
		return false;
	}
	
	public String getApplicationPodName() {
		return applicationPodName;
	}
}
