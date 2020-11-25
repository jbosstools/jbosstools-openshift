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
package org.jboss.tools.openshift.ui.bot.test.odo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftODOConnectionRequirement.CleanODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
import org.jboss.tools.openshift.ui.bot.test.AbstractODOTest;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Project management test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
@CleanODOConnection
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectManagementODOTest extends AbstractODOTest {
	String projectName = DatastoreOS3.TEST_PROJECT;
	
	@Test
	public void testCreateProject () {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
		OpenShiftODOProject prj  = connection.createNewProject(projectName);
		assertNotNull(prj);
		
		assertTrue(connection.projectExists(projectName));
	}
	
	@Test
	public void testDeleteProject () {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
		OpenShiftODOProject project = connection.getProject(projectName);
		project.delete();

		connection.refresh();
		assertFalse(connection.projectExists(projectName));
	}

}
