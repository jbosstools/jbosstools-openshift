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

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RequiredBasicConnection
@CleanConnection
@RunWith(RedDeerSuite.class)
public class CreateResourcesTest {

	private String testProject;
	private OpenShiftExplorerView explorer = new OpenShiftExplorerView();

	public static final String RESOURCES_LOCATION = System.getProperty("user.dir") + File.separator + "resources";
	private static final String POD_NAME = "hello-openshift";
	private static final String SERVICE_NAME = POD_NAME;
	private static final String ROUTE_NAME = POD_NAME;

	@Before
	public void prepareTestEnvironment() {
		testProject = "rsrc-app-project" + System.currentTimeMillis();
		explorer.getOpenShift3Connection().createNewProject(testProject);
	}

	@Test
	public void testOpenCreateResourceWizardViaContextMenuOfConnection() {
		explorer.getOpenShift3Connection().select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();

		assertResourceShellIsAvailable();
	}

	@Test
	public void testOpenCreateResourceWizardViaContextMenuOfProject() {
		explorer.getOpenShift3Connection().getProject(testProject).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();

		assertResourceShellIsAvailable();
	}

	@Test
	public void testCreateResourceFromLocalFile() {
		createResource(RESOURCES_LOCATION + File.separator + "hello-pod.json");
		refreshProject();

		assertTrue("Hello pod has not been created from file", explorer.getOpenShift3Connection()
				.getProject(testProject).getOpenShiftResource(Resource.POD, POD_NAME) != null);

		new WaitUntil(new OpenShiftResourceExists(Resource.POD, POD_NAME, ResourceState.RUNNING, testProject),
				TimePeriod.LONG);

		createResource(RESOURCES_LOCATION + File.separator + "hello-service.json");
		refreshProject();

		assertTrue("Hello service has not been created from file", explorer.getOpenShift3Connection()
				.getProject(testProject).getOpenShiftResource(Resource.SERVICE, SERVICE_NAME) != null);
		assertTrue("Hello service is not visible in OpenShift Explorer view", explorer.getOpenShift3Connection()
				.getProject(testProject).getService(SERVICE_NAME).getTreeItem() != null);

		createResource(RESOURCES_LOCATION + File.separator + "hello-route.json");
		refreshProject();

		assertTrue("Hello route has not been created from file", explorer.getOpenShift3Connection()
				.getProject(testProject).getOpenShiftResource(Resource.ROUTE, ROUTE_NAME) != null);
	}

	@Test
	public void testCreateResourceFromURL() {
		createResource(
				"https://raw.githubusercontent.com/jbosstools/jbosstools-openshift/master/itests/org.jboss.tools.openshift.ui.bot.test/resources/hello-pod.json");
		
		new WaitUntil(new OpenShiftResourceExists(Resource.POD, POD_NAME, ResourceState.RUNNING, testProject),
				TimePeriod.LONG);

		assertTrue("Hello pod has not been created from file", explorer.getOpenShift3Connection()
				.getProject(testProject).getOpenShiftResource(Resource.POD, POD_NAME) != null);
	}

	private void createResource(String pathToResource) {
		explorer.getOpenShift3Connection().getProject(testProject).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_RESOURCE).select();

		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);

		assertTrue("Selected project has not been selected for new resource creationg.",
				new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT).getSelection().equals(testProject));

		new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_LOCATION).setText(pathToResource);
		new FinishButton().click();

		new DefaultShell(OpenShiftLabel.Shell.CREATE_RESOURCE_SUMMARY);

		assertTrue("Resource is not listed in created resources summary", new DefaultTree().getAllItems().size() == 1);

		new OkButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	private void assertResourceShellIsAvailable() {
		try {
			new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
		} catch (WaitTimeoutExpiredException ex) {
			fail("New OpenShift resource shell has not been opened");
		}

		new DefaultShell(OpenShiftLabel.Shell.NEW_RESOURCE);
		new CancelButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_RESOURCE));
	}

	private void refreshProject() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		explorer.getOpenShift3Connection().getProject(testProject).refresh();

		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	@After
	public void recreateProject() {
		if (explorer.getOpenShift3Connection().projectExists(testProject)) {
			explorer.getOpenShift3Connection().getProject(testProject).delete();
		}
	}
}
