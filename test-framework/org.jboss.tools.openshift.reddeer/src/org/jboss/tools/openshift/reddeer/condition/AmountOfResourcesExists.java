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

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;

public class AmountOfResourcesExists extends AbstractWaitCondition {

	private static OpenShiftExplorerView explorer;
	private OpenShiftProject project;
	private Resource resource;
	private int amount;
	private Connection connection;
	
	public AmountOfResourcesExists(Resource resource, int amount, Connection connection) {
		this(resource, amount, null, connection);
	}

	public AmountOfResourcesExists(Resource resource, int amount, String projectName, Connection connection) {
		explorer = new OpenShiftExplorerView();
		if (projectName == null) {
			this.project = explorer.getOpenShift3Connection(connection).getProject();
		}else {
			this.project = explorer.getOpenShift3Connection(connection).getProject(projectName);
		}
		this.resource = resource;
		this.amount = amount;
	}
	
	@Override
	public boolean test() {
		// workaround for disposed project
		if (project.getTreeItem().isDisposed()) {
			project = explorer.getOpenShift3Connection(connection).getProject();
		}
		
		return project.getOpenShiftResources(resource).size() == amount;
	}

	@Override
	public String description() {
		return "Waiting for existence of " + amount + " " + resource.toString() + " resource(s).";  
	}
}
