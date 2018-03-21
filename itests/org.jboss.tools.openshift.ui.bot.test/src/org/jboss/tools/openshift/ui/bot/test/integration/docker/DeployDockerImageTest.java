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
package org.jboss.tools.openshift.ui.bot.test.integration.docker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.jface.exception.JFaceLayerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.junit.screenshot.CaptureScreenshotException;
import org.eclipse.reddeer.junit.screenshot.ScreenshotCapturer;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author mlabuda@redhat.com
 * @contributor jkopriva@redhat.com
 */
@OpenPerspective(JBossPerspective.class)
@OCBinary(setOCInPrefs = true, cleanup = false)
@RequiredBasicConnection
@CleanConnection
@RunWith(RedDeerSuite.class)
public class DeployDockerImageTest extends AbstractDockerImageTest {
	private static final Logger LOGGER = new Logger(DeployDockerImageTest.class);

	@InjectRequirement
	private static OpenShiftConnectionRequirement openshiftConnectionRequirement;

	public static String PROJECT1 = "deployimagetesting1" + System.currentTimeMillis();
	public static String PROJECT2 = "deployimagetesting2" + System.currentTimeMillis();

	private static final String HELLO_OS_DOCKER_IMAGE = "docker.io/openshift/hello-openshift";
	private static final String TAG = "v1.2.1";

	@BeforeClass
	public static void setUp() {
		closeBrowser();// try to close browser if it is opened
		createDockerConnection();
		try {
			pullImageIfDoesNotExist(HELLO_OS_DOCKER_IMAGE, TAG);
		} catch (JFaceLayerException ex) {
			String newExceptionMessage = debugDockerImageTest();
			throw new RuntimeException(newExceptionMessage, ex);
		}
		createProject(PROJECT1, openshiftConnectionRequirement);
		createProject(PROJECT2, openshiftConnectionRequirement);
	}

	/**
	 * Auxiliary method for helping with debugging JBIDE-23841. This method
	 * maximizes Docker explorer view, captures screenshot and restores the view
	 * back. It also gathers some info and returns it.
	 * 
	 */

	private static String debugDockerImageTest() {
		String message = "";
		DockerExplorerView dockerExplorerView = new DockerExplorerView();
		dockerExplorerView.maximize();
		try {
			ScreenshotCapturer.getInstance().captureScreenshot("DeployDockerImageTest#setup");
		} catch (CaptureScreenshotException e) {
			// Capturing screenshot was not successfull. No big deal.
			LOGGER.debug("Capturing screenshot was not succesfull.");
		}
		dockerExplorerView.restore();
		List<String> names = dockerExplorerView.getDockerConnectionNames();
		for (String name : names) {
			DockerConnection connection = dockerExplorerView.getDockerConnectionByName(name);
			TreeItem treeItem = connection.getTreeItem();
			message += "TreeItem for connection \"" + name + "\": " + treeItem.getText() + "\n";
		}
		return message;
	}

	@AfterClass
	public static void cleanUp() {
		OpenShift3Connection connection = new OpenShiftExplorerView()
				.getOpenShift3Connection(openshiftConnectionRequirement.getConnection());
		if (connection.projectExists(PROJECT1)) {
			connection.getProject(PROJECT1).delete();
		}
		if (connection.projectExists(PROJECT2)) {
			connection.getProject(PROJECT2).delete();
		}
	}

	@After
	public void cleanUpAfter() {
		closeBrowser();
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
		selectProject(PROJECT2, openshiftConnectionRequirement);
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DEPLOY_DOCKER_IMAGE).select();

		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);

		new LabeledText(OpenShiftLabel.TextLabels.IMAGE_NAME).setText(HELLO_OS_DOCKER_IMAGE + ":" + TAG);

		proceedThroughDeployImageToOpenShiftWizard();

		verifyDeployedDockerImageInBrowser(PROJECT2, "hello-openshift", "Hello OpenShift!", openshiftConnectionRequirement);
	}

	@Test
	public void testDeployDockerImageFromDockerExplorer() {
		selectProject(PROJECT1, openshiftConnectionRequirement);
		openDeployToOpenShiftWizardFromDockerExplorer(HELLO_OS_DOCKER_IMAGE, TAG);

		proceedThroughDeployImageToOpenShiftWizard();

		verifyDeployedDockerImageInBrowser(PROJECT1, "hello-openshift", "Hello OpenShift!", openshiftConnectionRequirement);
	}


	/**
	 * Selects project in OpenShift explorer view, open Deploy Image to OpenShift
	 * wizard from its context menu and assert that there are correct values.
	 * 
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void assertDockerImageIsProcessedCorrectlyWhenUsedFromOpenShiftExplorer(String projectName) {
		selectProject(projectName, openshiftConnectionRequirement);
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DEPLOY_DOCKER_IMAGE).select();

		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);

		assertFalse("No project has been preselected.",
				new LabeledCombo("OpenShift Project: ").getSelection().equals(""));
		assertTrue("Wrong project has been preselected.",
				new LabeledCombo("OpenShift Project: ").getSelection().equals(projectName));

		closeWizard();
	}

	/**
	 * Select project in OpenShift explorer and open Deploy Image to OpenShift
	 * wizard from Docker Explorer and check data processing and close wizard in the
	 * end.
	 * 
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void assertDockerImageIsProcessedCorrectlyWhenUsedFromDockerExplorer(String projectName) {
		selectProject(projectName, openshiftConnectionRequirement);
		openDeployToOpenShiftWizardFromDockerExplorer(HELLO_OS_DOCKER_IMAGE, TAG);
		selectProjectAndVerifyDataProcessingInDeployToOpenShiftWizard(projectName);
		closeWizard();
	}

	/**
	 * Selects a specified project and verify it is correctly processed in Deploy
	 * Image to OpenShift wizard as well as processing of docker image details.
	 * 
	 * @param projectName
	 * @param projectDisplayName
	 */
	private void selectProjectAndVerifyDataProcessingInDeployToOpenShiftWizard(String projectName) {
		assertTrue("Wrong project has been preselected.",
				new LabeledCombo("OpenShift Project: ").getSelection().equals(projectName));
		assertTrue("Selected docker image should be used in wizard but it is not.",
				new LabeledText(OpenShiftLabel.TextLabels.IMAGE_NAME).getText().contains(HELLO_OS_DOCKER_IMAGE));
		assertTrue("Resource should be infered from image name but it is not",
				new LabeledText(OpenShiftLabel.TextLabels.RESOURCE_NAME).getText()
						.contains(HELLO_OS_DOCKER_IMAGE.split("/")[2]));
	}
	
	

}
