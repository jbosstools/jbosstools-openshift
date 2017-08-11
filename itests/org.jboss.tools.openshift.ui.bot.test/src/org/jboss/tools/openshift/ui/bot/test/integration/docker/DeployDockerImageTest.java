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
package org.jboss.tools.openshift.ui.bot.test.integration.docker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.hamcrest.core.StringContains;
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.platform.RunningPlatform;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.ui.browser.BrowserEditor;
import org.jboss.reddeer.jface.exception.JFaceLayerException;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.junit.screenshot.CaptureScreenshotException;
import org.jboss.reddeer.junit.screenshot.ScreenshotCapturer;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.condition.TreeContainsItem;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.widget.ShellWithButton;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RequiredBasicConnection
@RunWith(RedDeerSuite.class)
public class DeployDockerImageTest {
	
	private static final Logger LOGGER = new Logger(DeployDockerImageTest.class);
	
	@InjectRequirement
	private static OpenShiftConnectionRequirement openshiftConnectionRequirement;
	
	private static DockerExplorerView dockerExplorer;
	
	public static String PROJECT1 = "deployimagetesting1" + System.currentTimeMillis();
	public static String PROJECT2 = "deployimagetesting2" + System.currentTimeMillis();
	
	public static String DOCKER_CONNECTION = "default";
	public static final String HELLO_OS_DOCKER_IMAGE = "docker.io/openshift/hello-openshift";
	public static final String TAG = "v1.2.1";
	
	@BeforeClass
	public static void setUp() {
		prepareDockerConnection();
		try {
			pullHelloImageIfDoesNotExist();
		} catch (JFaceLayerException ex) {
			String newExceptionMessage = debugDockerImageTest();
			throw new RuntimeException(newExceptionMessage, ex);
		}
		createProjects();
	}

	/**
	 * Auxiliary method for helping with debugging JBIDE-23841.
	 * This method maximizes Docker explorer view, captures screenshot and restores the view back.
	 * It also gathers some info and returns it.
	 * 
	 */
	
	private static String debugDockerImageTest() {
		String message = "";
		DockerExplorerView dockerExplorerView = new DockerExplorerView();
		dockerExplorerView.maximize();
		try {
			ScreenshotCapturer.getInstance().captureScreenshot("DeployDockerImageTest#setup");
		} catch (CaptureScreenshotException e) {
			//Capturing screenshot was not successfull. No big deal.
			LOGGER.debug("Capturing screenshot was not succesfull.");
		}
		dockerExplorerView.restore();
		List<String> names = dockerExplorerView.getDockerConnectionNames();
		for (String name : names) {
			DockerConnection connection = dockerExplorerView.getDockerConnectionByName(name);
			TreeItem treeItem = connection.getTreeItem();
			message += "TreeItem for connection \"" + name + "\": " + treeItem.getText()+"\n";
		}
		return message;
	}
	
	private static void prepareDockerConnection() {
		dockerExplorer = new DockerExplorerView();
		dockerExplorer.open();
		List<String> connectionsNames = dockerExplorer.getDockerConnectionNames();
		if (connectionsNames.isEmpty()) {
			if (RunningPlatform.isWindows() || RunningPlatform.isOSX()) {
				dockerExplorer.createDockerConnectionSearch(DOCKER_CONNECTION);
			} else {
				dockerExplorer.createDockerConnectionUnix(DOCKER_CONNECTION, 
						"unix:///var/run/docker.sock");
			}
		} else if (RunningPlatform.isLinux() && connectionsNames.size() > 1) {
			for (String name : connectionsNames) {
				if (name.startsWith("unix:///var/run/docker.sock")) {
					DOCKER_CONNECTION = name;
					break;
				}
			}
		} else {
			DOCKER_CONNECTION = connectionsNames.get(0);
		}
	}
	
	/**
	 * If hello world docker image does not exist, this method will pull it.
	 */
	private static void pullHelloImageIfDoesNotExist() {
		DockerExplorerView dockerExplorer = new DockerExplorerView();
		DockerConnection dockerConnection = dockerExplorer.getDockerConnectionByName(DOCKER_CONNECTION);
		
		dockerConnection.getTreeItem().expand();
		new WaitWhile(new TreeContainsItem(dockerConnection.getTreeItem().getParent(),
				dockerConnection.getTreeItem().getText(), "Loading..."),TimePeriod.LONG);
		
		if (dockerConnection.getImage(HELLO_OS_DOCKER_IMAGE, TAG) == null) {
			dockerConnection.pullImage(HELLO_OS_DOCKER_IMAGE, TAG);
		}
	}
	
	private static void createProjects() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShift3Connection connection = explorer.getOpenShift3Connection(
				openshiftConnectionRequirement.getConnection());
		connection.createNewProject(PROJECT1);
		connection.createNewProject(PROJECT2);
	}
	
	@AfterClass
	public static void cleanUp() {
		OpenShift3Connection connection  = new OpenShiftExplorerView().getOpenShift3Connection(
				openshiftConnectionRequirement.getConnection());
		connection.getProject(PROJECT1).delete();
		connection.getProject(PROJECT2).delete();
	}
	
	@After
	public void closeBrowser() {
		try {
			BrowserEditor browser = new BrowserEditor(new StringContains("hello"));
			browser.close();
		} catch (RedDeerException ex) {
			// do nothing, browser is not opened
		}
	}
	
	@Test
	public void testWizardDataHandlingOpenedFromOpenShiftExplorerTest() {
		assertDockerImageIsProcessedCorrectlyWhenUsedFromOpenShiftExplorer(PROJECT1);
		assertDockerImageIsProcessedCorrectlyWhenUsedFromOpenShiftExplorer(PROJECT2);
		assertDockerImageIsProcessedCorrectlyWhenUsedFromOpenShiftExplorer(PROJECT1);
	}
	
	@Test
	public void testWizardDataHandlingOpenedFromDockerExplorer() {
		assertDockerImageIsProcessedCorrectlyWhenUsedFromDockerExplorer(PROJECT1);
		assertDockerImageIsProcessedCorrectlyWhenUsedFromDockerExplorer(PROJECT2);		
		assertDockerImageIsProcessedCorrectlyWhenUsedFromDockerExplorer(PROJECT1);
	}
	
	@Test
	public void testDeployDockerImageFromOpenShiftExplorer() {
		selectProject(PROJECT2);
		new ContextMenu(OpenShiftLabel.ContextMenu.DEPLOY_DOCKER_IMAGE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);
		
		new LabeledText(OpenShiftLabel.TextLabels.IMAGE_NAME).setText(HELLO_OS_DOCKER_IMAGE + ":" + TAG);
		
		proceedThroughDeployImageToOpenShiftWizard();
		
		verifyDeployedHelloWorldDockerImage(PROJECT2);	
	}
		
	@Test
	public void testDeployDockerImageFromDockerExplorer() {
		selectProject(PROJECT1);
		openDeployToOpenShiftWizardFromDockerExplorer();

		proceedThroughDeployImageToOpenShiftWizard();
		
		verifyDeployedHelloWorldDockerImage(PROJECT1);	
	}
	
	/**
	 * Verifies whether an application pod has been created and application is running successfully.
	 */
	private void verifyDeployedHelloWorldDockerImage(String projectName) {
		new OpenShiftExplorerView().getOpenShift3Connection(
				openshiftConnectionRequirement.getConnection()).refresh();
		try {
			new WaitUntil(new OpenShiftResourceExists(Resource.POD, new StringContains("hello-openshift"),
				ResourceState.RUNNING, projectName), TimePeriod.VERY_LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("There should be a running application pod for a deployed docker image, "
					+ "but it does not exist.");
		}
		
		new OpenShiftExplorerView().getOpenShift3Connection().getProject(projectName).
				getOpenShiftResources(Resource.ROUTE).get(0).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.SHOW_IN_BROWSER).select();
		
		try {
			new WaitUntil(new BrowserContainsText("Hello OpenShift!"), TimePeriod.VERY_LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Browser does not containg hello world content.");
		}
	}
	
	/**
	 * Proceeds through the image if the first wizard page has correct details -
	 * connection, project and image name.
	 */
	private void proceedThroughDeployImageToOpenShiftWizard() {
		new WaitUntil(new WidgetIsEnabled(new NextButton()), TimePeriod.NORMAL, false);
		
		assertTrue("Next button should be enabled if all details are set correctly",
				new NextButton().isEnabled());
		
		new NextButton().click();
		
		new WaitUntil(new WidgetIsEnabled(new BackButton()), TimePeriod.LONG);
		
		new NextButton().click();
		
		if (!new CheckBox("Add Route").isChecked()) {
			new CheckBox("Add Route").click();
		}
		
		new FinishButton().click();
		
		new ShellWithButton("Deploy Image to OpenShift", "OK");
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable("Deploy Image to OpenShift"), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	/**
	 * Selects project in OpenShift explorer view, open Deploy Image to OpenShift wizard
	 * from its context menu and assert that there are correct values.
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void assertDockerImageIsProcessedCorrectlyWhenUsedFromOpenShiftExplorer(String projectName) {
		selectProject(projectName);
		new ContextMenu(OpenShiftLabel.ContextMenu.DEPLOY_DOCKER_IMAGE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);
		
		assertFalse("No project has been preselected.", new LabeledCombo("OpenShift Project: ").
				getSelection().equals(""));
		assertTrue("Wrong project has been preselected.", new LabeledCombo("OpenShift Project: ").
				getSelection().equals(projectName));
		
		closeWizard();
	}
	
	/**
	 * Select project in OpenShift explorer and open Deploy Image to OpenShift wizard
	 * from Docker Explorer and check data processing and close wizard in the end.
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void assertDockerImageIsProcessedCorrectlyWhenUsedFromDockerExplorer(String projectName) {
		selectProject(projectName);
		openDeployToOpenShiftWizardFromDockerExplorer();
		selectProjectAndVerifyDataProcessingInDeployToOpenShiftWizard(projectName);
		closeWizard();
	}	
	
	/**
	 * Selects a specified project and verify it is correctly processed in Deploy Image
	 * to OpenShift wizard as well as processing of docker image details.
	 * 
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void selectProjectAndVerifyDataProcessingInDeployToOpenShiftWizard(String projectName) {
		assertTrue("Wrong project has been preselected.", new LabeledCombo("OpenShift Project: ").
				getSelection().equals(projectName));		
		assertTrue("Selected docker image should be used in wizard but it is not.",
				new LabeledText(OpenShiftLabel.TextLabels.IMAGE_NAME).getText().contains(
						HELLO_OS_DOCKER_IMAGE));
		assertTrue("Resource should be infered from image name but it is not",
				new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_NAME).getText().contains(
						HELLO_OS_DOCKER_IMAGE.split("/")[2]));
	}
	
	/**
	 * Opens a Deploy Image to OpenShift wizard from context menu of a docker image
	 */
	private void openDeployToOpenShiftWizardFromDockerExplorer() {
		dockerExplorer.getDockerConnectionByName(DOCKER_CONNECTION).getImage(
				HELLO_OS_DOCKER_IMAGE, TAG).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.DEPLOY_TO_OPENSHIFT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);
	}
	
	/**
	 * Closes Deploy Image to OpenShift wizard.
	 */
	private void closeWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT));
	}
	
	/**
	 * Selects project in OpenShift explorer view.
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void selectProject(String projectName) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.getOpenShift3Connection(openshiftConnectionRequirement.getConnection()).
			getProject(projectName).select();
	}
	
}
