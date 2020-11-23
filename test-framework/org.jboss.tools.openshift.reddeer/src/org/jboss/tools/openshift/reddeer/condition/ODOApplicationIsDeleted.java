/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOApplication;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;

/**
 * Wait condition to wait for OpenShift ODO Project is deleted.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class ODOApplicationIsDeleted extends AbstractWaitCondition {
	
	private String projectName;
	private String applicationName;

	/**
	 * Constructs OODOProjectIsDeleted wait condition. Condition is met when project is deleted.
	 * 
	 * @param projectName project name
	 */
	public ODOApplicationIsDeleted(String projectName, String applicationName) {
		this.projectName = projectName;
		this.applicationName = applicationName;
	}
	
	@Override
	public boolean test() {
		try {
			OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
			explorer.open();
			OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
			connection.refreshConnection();
			OpenShiftODOProject project = connection.getProject(projectName);
			if (project == null) {
				return true;
			} else {
			  OpenShiftODOApplication application = project.getApplication(applicationName);
			  if (application == null) {
			    return true;
			  } 
				return false;
			}
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "ODO application with name:"+ applicationName +" in project "+ projectName + " is deleted";
	}
}
