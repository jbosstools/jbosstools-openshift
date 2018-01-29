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
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
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
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection
public class HandleCustomTemplateTest extends AbstractTest {

	@Before
	public void setUp() {
		// If project does not exists, e.g. something went south in recreation earlier, create it
		if (!new OpenShiftProjectExists(DatastoreOS3.PROJECT1_DISPLAYED_NAME).test()) {
			new OpenShiftExplorerView().getOpenShift3Connection().createNewProject();
		}
	}
	
	@Test
	public void testCreateTemplateFromFileAndUseItInWizard() {
		createTemplateFromJson();
		
		assertTemplateIsUsableInApplicationWizard();
	}
	
	private void assertTemplateIsUsableInApplicationWizard() {
		new NewOpenShift3ApplicationWizard().openWizardFromExplorer();
		
		try {
			new DefaultTreeItem("helloworld-sample (instant-app) - " + DatastoreOS3.PROJECT1);
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
		
		explorer.getOpenShift3Connection().getProject().select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);
		new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_LOCATION).setText(
				System.getProperty("user.dir") + File.separator + "resources" + 
						File.separator + "hello-world-template.json");
		new FinishButton().click();
		 
		new DefaultShell(OpenShiftLabel.Shell.CREATE_RESOURCE_SUMMARY);
		
		assertTrue("Template is not listed in created resources summary",
				new DefaultTree().getAllItems().size() == 1);
		
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		List<OpenShiftResource> templates = explorer.getOpenShift3Connection().getProject().
				getOpenShiftResources(Resource.TEMPLATE);
		assertTrue("There should be precisely 1 created template for the project.",
				templates.size() > 0);
		
		String templateName = templates.get(0).getName();
		assertTrue("Template name '" + templateName + "' does not match required name "
				+ "helloworld-sample.", templateName.equals("helloworld-sample"));
	}
	
	@After
	public void cleanUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection  = explorer.getOpenShift3Connection();
		connection.getProject().delete();
		
		try {
			new WaitWhile(new OpenShiftProjectExists());
		} catch (WaitTimeoutExpiredException ex) {
			connection.refresh();
		
			new WaitWhile(new OpenShiftProjectExists(), TimePeriod.getCustom(5));
		}
		
		connection.createNewProject();
	}
}
