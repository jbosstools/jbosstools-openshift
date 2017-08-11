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

import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.enums.Resource;
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
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Class for resources being shown correctly in OS Explorer
 * @author Pavol Srna
 *
 */
@RunWith(RedDeerSuite.class)
@OCBinary
@RequiredBasicConnection
@CleanConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE)
public class OSExplorerResourceTest {
	
	public static String GIT_FOLDER = "jboss-eap-quickstarts";
	public static String PROJECT_NAME = "jboss-helloworld";
	public static String BUILD_CONFIG = "eap-app";

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
	 * Test if deployment config is visible in OS Explorer, when there is no service.
	 */
	@Test(expected=OpenshiftTestInFailureException.class)
	public void testDeploymentConfigVisibleAfterServiceDeletion(){
		this.project.expand();
		Service service = this.project.getService(OpenShiftResources.EAP_SERVICE);
		assertTrue("Service does not exist!", service != null);
		service.select();
		new ContextMenu("Delete").select();
		new DefaultShell("Delete OpenShift Resource");
		new OkButton().click();
		//assert service is deleted
		List<OpenShiftResource> resources = this.project.getOpenShiftResources(Resource.SERVICE);
		assertTrue("Service not deleted!", resources.isEmpty());
		try{
			this.project.getTreeItem().getItem("eap-app selector: deploymentConfig=eap-app");
		}catch (CoreLayerException e) {
			// TODO: do not throw after JBIDE-24217 is fixed
			throw new OpenshiftTestInFailureException("JBIDE-24217");
			// TODO: uncomment after JBIDE-24217 is fixed
			//fail("Deployment config not visible!");
		}
			
	}
	
	@After
	public void cleanup(){
		cleanReq.fulfill();
	}
}
