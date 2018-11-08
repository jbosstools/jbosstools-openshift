/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Class for resources being shown correctly in OS Explorer
 * 
 * @author Pavol Srna
 *
 */
@RunWith(RedDeerSuite.class)
@OCBinary(setOCInPrefs = true)
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE)
public class OSExplorerResourceTest extends AbstractTest {

	@InjectRequirement
	private CleanOpenShiftConnectionRequirement cleanReq;
	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;
	private OpenShift3Connection connection;
	private OpenShiftProject project;

	@Before
	public void setUp() {
		TestUtils.setUpOcBinary();
		TestUtils.cleanupGitFolder(OpenShiftResources.NODEJS_GIT_NAME);
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		this.connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());
		this.project = connection.getProject(requiredProject.getProjectName());
	}

	/**
	 * Test if deployment config is visible in OS Explorer, when there is no
	 * service.
	 */
	@Test(expected = OpenshiftTestInFailureException.class)
	public void testDeploymentConfigVisibleAfterServiceDeletion() {
		deleteService(OpenShiftResources.EAP_SERVICE);
		deleteService(OpenShiftResources.EAP_SERVICE_PING);
		// assert services are deleted
		List<OpenShiftResource> resources = this.project.getOpenShiftResources(Resource.SERVICE);
		assertTrue("Service not deleted!", resources.isEmpty());
		try {
			this.project.getTreeItem().getItem("eap-app selector: deploymentConfig=eap-app");
		} catch (CoreLayerException e) {
			// TODO: do not throw after JBIDE-24217 is fixed
			throw new OpenshiftTestInFailureException("JBIDE-24217");
			// TODO: uncomment after JBIDE-24217 is fixed
			// fail("Deployment config not visible!");
		}
	}

	private void deleteService(String serviceName) {
		this.project.expand();
		Service service = this.project.getServicesWithName(serviceName).get(0);
		assertTrue("Service does not exist!", service != null);
		service.select();
		new ContextMenuItem("Delete").select();
		new DefaultShell("Delete OpenShift Resource");
		new OkButton().click();
		new WaitWhile(new OpenShiftResourceExists(Resource.SERVICE, serviceName, ResourceState.UNSPECIFIED,
				project.getName(), requiredConnection.getConnection()));
	}

	@After
	public void cleanup() {
		cleanReq.fulfill();
	}
}
