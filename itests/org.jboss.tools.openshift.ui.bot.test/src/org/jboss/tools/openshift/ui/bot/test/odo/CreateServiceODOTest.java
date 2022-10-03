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

import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftODOConnectionRequirement.CleanODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement.RequiredODOProject;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOApplication;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
import org.jboss.tools.openshift.reddeer.widget.terminal.TerminalHasNoChange;
import org.jboss.tools.openshift.reddeer.wizard.CreateServiceWizard;
import org.jboss.tools.openshift.reddeer.wizard.page.CreateServiceWizadPage;
import org.jboss.tools.openshift.ui.bot.test.AbstractODOTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Create Service test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
@CleanODOConnection
@RequiredODOProject(name = "test-project7", cleanup = true)
public class CreateServiceODOTest extends AbstractODOTest {
	
	private static final String ECLIPSE_PROJECT = "myservice"; 
	
	private static final String APPLICATION_NAME = "myapp";
	
	@InjectRequirement
	private static OpenShiftODOProjectRequirement projectReq;
	
	
	@BeforeClass
	public static void setupWorkspace() {
		importEmptyProject(ECLIPSE_PROJECT);
		createComponent(ECLIPSE_PROJECT, projectReq.getProjectName(), "node.js", "nodejs-starter", true);
	}
	
	@Test
	public void testCreateService() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOProject project = explorer.getOpenShiftODOConnection().getProject(projectReq.getProjectName());
		project.openCreateServiceWizard();
		try {
			new DefaultShell("Create service");
			new OkButton().click();
			fail("Service catalog is not installed");
		} catch (RedDeerException ex) {
			//swallow Service catalog is working
		}
		CreateServiceWizard serviceWizard = new CreateServiceWizard();
		CreateServiceWizadPage serviceWizardPage = new CreateServiceWizadPage(serviceWizard);
		serviceWizardPage.setServiceName("myapp");
		serviceWizardPage.setService("MongoDB Operator");
		serviceWizardPage.setComponentType("MongoDB");
		serviceWizardPage.setApplication("myapp");
		serviceWizard.finish(TimePeriod.LONG);
		
		OpenShiftODOApplication application = project.getApplication(APPLICATION_NAME);
		application.expand();
		List<TreeItem> items = application.getTreeItem().getItems();
		new WaitWhile(new TerminalHasNoChange(), TimePeriod.VERY_LONG);
	}

}
