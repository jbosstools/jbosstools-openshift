/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.integration.docker;

import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
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
 * @author jkopriva@redhat.com
 */
@OpenPerspective(JBossPerspective.class)
@OCBinary(setOCInPrefs = true, cleanup = false)
@RequiredBasicConnection
@RunWith(RedDeerSuite.class)
public class DeployVariousDockerImagesTest extends AbstractDockerImageTest {

	@InjectRequirement
	private static OpenShiftConnectionRequirement openshiftConnectionRequirement;

	private static final String PROJECT_NEXUS = "nexus" + System.currentTimeMillis();
	private static final String NEXUS_DOCKER_IMAGE = "sonatype/nexus";
	private static final String NEXUS_TAG = "2.14.8";

	private static final String PROJECT_SPRING_HELLOWORLD = "springhelloworld" + System.currentTimeMillis();
	private static final String SPRINGBOOT_DOCKER_IMAGE = "saturnism/spring-boot-helloworld-ui";
	private static final String SPRINGBOOT_TAG = "latest";

	@BeforeClass
	public static void setUp() {
		closeBrowser();// try to close browser if it is opened
		createDockerConnection();
		pullImageIfDoesNotExist(NEXUS_DOCKER_IMAGE, NEXUS_TAG);
		pullImageIfDoesNotExist(SPRINGBOOT_DOCKER_IMAGE, SPRINGBOOT_TAG);
		createProject(PROJECT_NEXUS, openshiftConnectionRequirement);
		createProject(PROJECT_SPRING_HELLOWORLD, openshiftConnectionRequirement);
	}

	@AfterClass
	public static void cleanUp() {
		OpenShift3Connection connection = new OpenShiftExplorerView()
				.getOpenShift3Connection(openshiftConnectionRequirement.getConnection());
		if (connection.projectExists(PROJECT_NEXUS)) {
			connection.getProject(PROJECT_NEXUS).delete();
		}
		if (connection.projectExists(PROJECT_SPRING_HELLOWORLD)) {
			connection.getProject(PROJECT_SPRING_HELLOWORLD).delete();
		}
	}

	@After
	public void cleanUpAfter() {
		closeBrowser();
	}

	@Test
	public void testDeployNexusDockerImageFromOpenShiftExplorer() {
		selectProject(PROJECT_NEXUS, openshiftConnectionRequirement);
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DEPLOY_DOCKER_IMAGE).select();

		new DefaultShell(OpenShiftLabel.Shell.DEPLOY_IMAGE_TO_OPENSHIFT);

		new LabeledText(OpenShiftLabel.TextLabels.IMAGE_NAME).setText(NEXUS_DOCKER_IMAGE + ":" + NEXUS_TAG);

		proceedThroughDeployImageToOpenShiftWizard();

		verifyDeployedDockerImageInBrowser(PROJECT_NEXUS, "nexus", "HTTP ERROR: 404", openshiftConnectionRequirement);
	}

	@Test
	public void testDeploySpringHelloWorldDockerImageFromDockerExplorer() {
		selectProject(PROJECT_SPRING_HELLOWORLD, openshiftConnectionRequirement);
		openDeployToOpenShiftWizardFromDockerExplorer(SPRINGBOOT_DOCKER_IMAGE, SPRINGBOOT_TAG);

		proceedThroughDeployImageToOpenShiftWizard(SPRINGBOOT_DOCKER_IMAGE + ":" + SPRINGBOOT_TAG);

		verifyDeployedDockerImageInBrowser(PROJECT_SPRING_HELLOWORLD, "spring-boot-helloworld-u", "message",
				openshiftConnectionRequirement);
	}

}
