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
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@RequiredBasicConnection
@CleanConnection
@RequiredProject
public class HandleCustomTemplateTest extends AbstractTest {

	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@Test
	public void testCreateTemplateFromFileAndUseItInWizard() {
		createTemplateFromJson();
		
		assertTemplateIsUsableInApplicationWizard();
	}
	
	private void assertTemplateIsUsableInApplicationWizard() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(projectReq.getProjectName());
		
		try {
			new DefaultTreeItem("helloworld-sample (instant-app) - " + projectReq.getProjectName());
		} catch (RedDeerException ex) {
			fail("Template is not visible in New OpenShift application wizard although it should be.");
		}
		
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	private void createTemplateFromJson() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);
		new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_LOCATION).setText(
				getTemplatePath());
		new FinishButton().click();
		 
		new DefaultShell(OpenShiftLabel.Shell.CREATE_RESOURCE_SUMMARY);
		
		assertTrue("Template is not listed in created resources summary",
				new DefaultTree().getAllItems().size() == 1);
		
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	
		explorer.getOpenShift3Connection(connectionReq.getConnection()).refresh();
		List<OpenShiftResource> templates = explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject(projectReq.getProjectName()).
				getOpenShiftResources(Resource.TEMPLATE);
		assertTrue("There should be precisely 1 created template for the project.",
				templates.size() > 0);
		
		String templateName = templates.get(0).getName();
		assertTrue("Template name '" + templateName + "' does not match required name "
				+ "helloworld-sample.", templateName.equals("helloworld-sample"));
	}
	
	protected String getTemplatePath() {
		return System.getProperty("user.dir") + File.separator + "resources" + 
				File.separator + "hello-world-template.json";
	}
	
}
