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
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;

/**
 * Wait condition to wait for OpenShift ODO Project is deleted.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class ODOProjectIsDeleted extends AbstractWaitCondition {
	
	private String projectName;

	/**
	 * Constructs OODOProjectIsDeleted wait condition. Condition is met when project is deleted.
	 * 
	 * @param projectName project name
	 */
	public ODOProjectIsDeleted(String projectName) {
		this.projectName = projectName;
	}
	
	@Override
	public boolean test() {
		try {
			OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
			explorer.open();
			OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
			connection.refreshConnection();
			OpenShiftODOProject project = connection.getProject(projectName);
			return project == null;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "ODO project with name:"+ projectName +" is deleted";
	}
}
