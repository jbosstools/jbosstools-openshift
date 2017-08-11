/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
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

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@RequiredBasicConnection
public class CreateResourcesTest {

	private String testProject;
	private OpenShiftExplorerView explorer = new OpenShiftExplorerView();
	
	public static final String RESOURCES_LOCATION = System.getProperty("user.dir") + 
			File.separator + "resources";
	
	@Before
	public void prepareTestEnvironment() {
		testProject = "rsrc-app-project" + System.currentTimeMillis();
		explorer.getOpenShift3Connection().createNewProject(testProject);
	}
	
	@Test
	public void testOpenCreateResourceWizardViaContextMenuOfConnection() {
		explorer.getOpenShift3Connection().select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();
		
		assertResourceShellIsAvailable();
	}
	
	@Test
	public void testOpenCreateResourceWizardViaContextMenuOfProject() {
		explorer.getOpenShift3Connection().getProject(testProject).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();
		
		assertResourceShellIsAvailable();
	}
	
	@Test
	public void testCreateResourceFromLocalFile() {
		createResource(RESOURCES_LOCATION + File.separator + "hello-pod.json");
		refreshProject();
		
		assertTrue("Hello pod has not been created from file",
				explorer.getOpenShift3Connection().getProject(testProject).
				getOpenShiftResource(Resource.POD, "hello-openshift") != null);
		
		createResource(RESOURCES_LOCATION + File.separator + "hello-service.json");
		refreshProject();
		
		assertTrue("Hello service has not been created from file",
				explorer.getOpenShift3Connection().getProject(testProject).
				getOpenShiftResource(Resource.SERVICE, "hello-openshift") != null);
		assertTrue("Hello service is not visible in OpenShift Explorer view",
				explorer.getOpenShift3Connection().getProject(testProject).
				getService("hello-openshift").getTreeItem() != null);
		
		createResource(RESOURCES_LOCATION + File.separator + "hello-route.json");
		refreshProject();
		
		assertTrue("Hello route has not been created from file", 
				explorer.getOpenShift3Connection().getProject(testProject).
				getOpenShiftResource(Resource.ROUTE, "hello-openshift") != null);
	}
	
	@Test
	public void testCreateResourceFromURL() {
		createResource("https://raw.githubusercontent.com/jbosstools/jbosstools-integration-tests/"
				+ "master/tests/org.jboss.tools.openshift.ui.bot.test/resources/hello-pod.json");
		
		assertTrue("Hello pod has not been created from file",
				explorer.getOpenShift3Connection().getProject(testProject).
				getOpenShiftResource(Resource.POD, "hello-openshift") != null);
	}
	
	private void createResource(String pathToResource) {
		explorer.getOpenShift3Connection().getProject(testProject).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);
		
		assertTrue("Selected project has not been selected for new resource creationg.",
				new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT).getSelection().equals(testProject));
		 
		new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_LOCATION).setText(pathToResource);
		new FinishButton().click();
		 
		new DefaultShell(OpenShiftLabel.Shell.CREATE_RESOURCE_SUMMARY);
		
		assertTrue("Resource is not listed in created resources summary",
				new DefaultTree().getAllItems().size() == 1);
		
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	private void assertResourceShellIsAvailable() {
		try {
			new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		} catch (WaitTimeoutExpiredException ex) {
			fail("New OpenShift resource shell has not been opened");
		}
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);
		new CancelButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
	}
	
	private void refreshProject() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		explorer.getOpenShift3Connection().getProject(testProject).refresh();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);		
	}
	
	@After
	public void recreateProject() {
		explorer.getOpenShift3Connection().getProject(testProject).delete();
	}
}
