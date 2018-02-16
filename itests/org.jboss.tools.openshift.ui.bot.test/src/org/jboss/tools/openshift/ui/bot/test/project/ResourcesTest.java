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
package org.jboss.tools.openshift.ui.bot.test.project;

import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RunWith(RedDeerSuite.class)
public class ResourcesTest extends AbstractTest {

	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;

	@Test
	public void testResourcesExistence() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShiftProject project = explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(DatastoreOS3.TEST_PROJECT);
		project.expand();
		TreeItem projectItem = project.getTreeItem();

		verifyResourceExistence(projectItem, Resource.BUILD_CONFIG);
		verifyResourceExistence(projectItem, Resource.BUILD);
		verifyResourceExistence(projectItem, Resource.DEPLOYMENT_CONFIG);
		verifyResourceExistence(projectItem, Resource.IMAGE_STREAM);
		verifyResourceExistence(projectItem, Resource.POD);
		verifyResourceExistence(projectItem, Resource.ROUTE);
		verifyResourceExistence(projectItem, Resource.SERVICE);
	}

	private void verifyResourceExistence(TreeItem projectItem, Resource resource) {
		try {
			OpenShiftProject project = new OpenShiftProject(projectItem);
			project.select();
			project.openProperties();
			project.selectTabbedProperty(resource.toString());
		} catch (RedDeerException ex) {
			fail("Resource " + resource.toString() + " does not exist under a specified project item.");
		}
	}
}
