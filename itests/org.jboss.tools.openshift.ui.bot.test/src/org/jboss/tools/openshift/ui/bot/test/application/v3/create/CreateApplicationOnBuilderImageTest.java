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
package org.jboss.tools.openshift.ui.bot.test.application.v3.create;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.reddeer.wizard.v3.NewOpenShift3ApplicationWizard;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.BuilderImageApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenshiftTestInFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@CleanConnection
public class CreateApplicationOnBuilderImageTest extends AbstractTest {

	private String gitFolder = "httpd-ex";
	private String projectName = "httpd-ex";
	private String applicationName;

	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@Before
	public void setUp() {
		OpenShift3NativeProjectUtils.getOrCreateProject(DatastoreOS3.PROJECT1,
				DatastoreOS3.PROJECT1_DISPLAYED_NAME, StringUtils.EMPTY, connectionReq.getConnection());
		TestUtils.cleanupGitFolder(gitFolder);
		if (new ProjectExists(projectName).test()) {
			new ProjectExplorer().getProject(projectName).delete(true);
		}
	}

	@Test
	public void testCreateApplicationBasedOnBuilderImage() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();

		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromExplorer(DatastoreOS3.PROJECT1_DISPLAYED_NAME);

		BuilderImageApplicationWizardHandlingTest.nextToBuildConfigurationWizardPage();

		applicationName = new LabeledText("Name: ").getText();

		new WaitUntil(new ControlIsEnabled(new FinishButton()));

		new FinishButton().click();

		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.APPLICATION_SUMMARY), TimePeriod.LONG);

		new DefaultShell(OpenShiftLabel.Shell.APPLICATION_SUMMARY);
		new OkButton().click();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.APPLICATION_SUMMARY));

		CreateApplicationFromTemplateTest.importApplicationAndVerify(projectName);

		OpenShiftProject project = explorer.getOpenShift3Connection(connectionReq.getConnection()).getProject();
		project.refresh();

		new WaitWhile(new JobIsRunning(), TimePeriod.getCustom(120));
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD_CONFIG, connectionReq.getConnection()), TimePeriod.LONG, false);

		List<OpenShiftResource> buildConfig = project.getOpenShiftResources(Resource.BUILD_CONFIG);
		assertTrue("There should be precisely 1 build config for created application, but there is following amount"
				+ " of build configs: " + buildConfig.size(), buildConfig.size() == 1);

		List<OpenShiftResource> imageStream = project.getOpenShiftResources(Resource.IMAGE_STREAM);
		assertTrue("There should be precisely 1 image stream for created application, but there is following amount"
				+ " of image streams: " + imageStream.size(), imageStream.size() == 1);

		List<OpenShiftResource> routes = project.getOpenShiftResources(Resource.ROUTE);
		assertTrue("There should be precisely 1 route for created application, but there is following amount"
				+ " of routes:" + routes.size(), routes.size() == 1);
		assertTrue("Generated (default) route should contain application name, but it's not contained.",
				routes.get(0).getName().equals(applicationName));

		List<OpenShiftResource> services = project.getOpenShiftResources(Resource.SERVICE);
		assertTrue("There should be precisely 1 service for created application, but there is following amount"
				+ " of services: " + services.size(), services.size() == 1);
	}

	private void validateJBIDE22704() {
		BuilderImageApplicationWizardHandlingTest.nextToBuildConfigurationWizardPage();

		applicationName = new LabeledText("Name: ").getText();

		assertNotNull(applicationName);
		assertTrue(applicationName.length() > 0);

		String gitUrl = new LabeledText("Git Repository URL:").getText();

		assertNotNull(gitUrl);
		if (gitUrl.length() < 1){
			new CancelButton().click();
			new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD));
			throw new OpenshiftTestInFailureException("JBIDE-23704 should be fixed now.");
		}

		new WaitUntil(new ControlIsEnabled(new NextButton()));

		/*
		 * switch to the Deployment page
		 */
		new NextButton().click();
		new WaitUntil(new ControlIsEnabled(new BackButton()));

		int numberofEnvironmentVariables = new DefaultTable().rowCount();
		assertTrue(numberofEnvironmentVariables > 0);

		/*
		 * switch to the Routing page
		 */
		new NextButton().click();
		new WaitUntil(new ControlIsEnabled(new BackButton()));

		int numberOfServicePorts = new DefaultTable().rowCount();
		assertTrue(numberOfServicePorts > 0);

		new CancelButton().click();
		new WaitWhile(new JobIsRunning(), TimePeriod.getCustom(120));
	}

	@Test
	public void validateJBIDE22704FromShellMenu() {
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromShellMenu();
		validateJBIDE22704();
	}

	@Test
	public void validateJBIDE22704FromCentral() {
		closeCentral();
		new NewOpenShift3ApplicationWizard(connectionReq.getConnection()).openWizardFromCentral();
		validateJBIDE22704();
	}

	@After
	public void tearDown() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();

		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.refresh();
		connection.getProject(DatastoreOS3.PROJECT1_DISPLAYED_NAME).delete();

		ProjectExplorer projectExplorer = new ProjectExplorer();
		if (projectExplorer.containsProject(projectName)) {
			projectExplorer.getProject(projectName).delete();
		}
		
		connection.refresh();
		
		//Close all shells if test fails (could interfere next tests)
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
	}
	
	private static void closeCentral() {
		new DefaultEditor(OpenShiftLabel.Others.RED_HAT_CENTRAL).close();
	}
}
