/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.AmountOfResourcesExists;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceOpenShift;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.wizard.v3.DeleteResourcesWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Class for testing: allow intellingent "Delete Resource"
 * Link: https://issues.jboss.org/browse/JBIDE-25111
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@OpenPerspective(value = JBossPerspective.class)
@OCBinary(setOCInPrefs = true)
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE_RESOURCES_PATH)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InteligentDeleteResourceTest extends AbstractTest {
	
	private static final String POD_NAME = OpenShiftResources.EAP_SERVICE;

	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;

	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;

	@BeforeClass
	public static void waitForApplication() {
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.COMPLETE,
				projectReq.getProjectName(), connectionReq.getConnection()), TimePeriod.getCustom(600), true);

		new WaitUntil(new AmountOfResourcesExists(Resource.POD, 2, projectReq.getProjectName(),
				connectionReq.getConnection()), TimePeriod.VERY_LONG, true);
	}
	
	@Test
	public void testASearchIsWorking() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftProject openshiftProject = explorer.getOpenShift3Connection(connectionReq.getConnection())
				.getProject(projectReq.getProjectName());
		openshiftProject.refresh();

		DeleteResourcesWizard deleteResourcesWizard = new DeleteResourcesWizard(connectionReq.getConnection());
		deleteResourcesWizard.openWizardFromExplorer(projectReq.getProjectName());
		
		int sizeBefore = deleteResourcesWizard.getAllResources().size();
		deleteResourcesWizard.setFilter(OpenShiftResources.EAP_SERVICE);
		assertNotEquals(sizeBefore, deleteResourcesWizard.getAllResources().size());
		deleteResourcesWizard.setFilter("");
		assertEquals(sizeBefore, deleteResourcesWizard.getAllResources().size());
		deleteResourcesWizard.cancel();
	}

	@Test
	public void testDeleteABuild() {
		deleteResource(ResourceOpenShift.BUILD);
		checkDeletedResource(ResourceOpenShift.BUILD);
	}

	@Test
	public void testDeleteBBuildConfig() {
		deleteResource(ResourceOpenShift.BUILD_CONFIG);
		checkDeletedResource(ResourceOpenShift.BUILD_CONFIG);
	}

	@Test
	public void testDeleteCDeploymentConfig() {
		deleteResource(ResourceOpenShift.DEPLOYMENT_CONFIG);
		checkDeletedResource(ResourceOpenShift.DEPLOYMENT_CONFIG);
	}

	@Test
	public void testDeleteGImageStream() {
		deleteResource(ResourceOpenShift.IMAGE_STREAM);
		checkDeletedResource(ResourceOpenShift.IMAGE_STREAM);
	}

	@Test
	public void testDeleteDService() {
		deleteResource(ResourceOpenShift.SERVICE); //eap-app
		deleteResource(ResourceOpenShift.SERVICE); //eap-app-ping
		checkDeletedResource(ResourceOpenShift.SERVICE);
	}

	@Test
	public void testDeleteFPod() {
		deleteResource(ResourceOpenShift.POD);

		new WaitWhile(new OpenShiftResourceExists(Resource.POD, POD_NAME, ResourceState.RUNNING, projectReq.getProjectName(), connectionReq.getConnection()),
				TimePeriod.LONG);
		if (!checkDeletedResourceBoolean(ResourceOpenShift.POD)) {
			//Wait once again - it could take some time on some machines
			new WaitWhile(new OpenShiftResourceExists(Resource.POD, POD_NAME, ResourceState.RUNNING, projectReq.getProjectName(), connectionReq.getConnection()),
					TimePeriod.LONG);
		}
		assertTrue("Resources are not deleted: " + ResourceOpenShift.POD.toString(), checkDeletedResourceBoolean(ResourceOpenShift.POD));
	}

	@Test
	public void testDeleteIRoute() {
		deleteResource(ResourceOpenShift.ROUTE);
		checkDeletedResource(ResourceOpenShift.ROUTE);
	}
	
	@Test
	public void testDeleteEReplicationController() {
		deleteResource(ResourceOpenShift.REPLICATION_CONTROLLER);
		checkDeletedResource(ResourceOpenShift.REPLICATION_CONTROLLER);
	}

	private void deleteResource(ResourceOpenShift resource) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftProject openshiftProject = explorer.getOpenShift3Connection(connectionReq.getConnection())
				.getProject(projectReq.getProjectName());
		openshiftProject.refresh();

		DeleteResourcesWizard deleteResourcesWizard = new DeleteResourcesWizard(connectionReq.getConnection());
		deleteResourcesWizard.openWizardFromExplorer(projectReq.getProjectName());

		List<TableItem> items = deleteResourcesWizard.getResourcesByType(resource);

		for (TableItem item : items) {
			item.select();
		}
		deleteResourcesWizard.delete();
		new WaitUntil(new JobIsRunning(), false);
	}

	private void checkDeletedResource(ResourceOpenShift resource) {
		assertTrue("Resources are not deleted: " + resource.toString(), checkDeletedResourceBoolean(resource));
	}
	
	private boolean checkDeletedResourceBoolean(ResourceOpenShift resource) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		OpenShiftProject openshiftProject = explorer.getOpenShift3Connection(connectionReq.getConnection())
				.getProject(projectReq.getProjectName());
		openshiftProject.refresh();

		DeleteResourcesWizard deleteResourcesWizard = new DeleteResourcesWizard(connectionReq.getConnection());
		deleteResourcesWizard.openWizardFromExplorer(projectReq.getProjectName());

		List<TableItem> items = deleteResourcesWizard.getResourcesByType(resource);
		
		deleteResourcesWizard.cancel();

		return items.isEmpty();
	}
	
	@After
	public void closeShell() {
		try {
			new CancelButton().click();
		} catch (CoreLayerException ex) {
			//swallow - it is not opened
		}
	}
}
