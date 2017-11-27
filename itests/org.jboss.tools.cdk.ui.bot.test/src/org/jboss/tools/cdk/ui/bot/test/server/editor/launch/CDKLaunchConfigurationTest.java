/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.editor.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration.CDKLaunchConfigurationDialog;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerContainerWizardPage;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDKServerEditorAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class for CDK server editor's launch configuration
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDKLaunchConfigurationTest extends CDKServerEditorAbstractTest {

	private String hypervisor = MINISHIFT_HYPERVISOR;
	
	private CDKLaunchConfigurationDialog launchDialog; 
	
	private static final Logger log = Logger.getLogger(CDKLaunchConfigurationTest.class);
	
	@BeforeClass
	public static void setUpEnvironment() {
		checkMinishiftProfileParameters();
	}
	
	@Before
	public void setup() {
		this.hypervisor = MINISHIFT_HYPERVISOR;
		assertCDKServerWizardFinished();
		setServerEditor();
		openLaunchConfiguration();
	}
	
	@After
	public void tearDownLaunchConfig() {
		closeLaunchConfig();
	}
	
	@AfterClass
	public static void tearDownEnvironment() {
		CDKTestUtils.removeAccessRedHatCredentials(CREDENTIALS_DOMAIN, USERNAME);
	}
	
	@Override
	protected String getServerAdapter() {
		return "Container Development Environment 3.2";
	}

	@Override
	public void setServerEditor() {
		serversView = new CDEServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CDK32ServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}

	@Override
	protected void setupServerWizardPage(NewMenuWizard dialog) {
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		page.selectType(SERVER_TYPE_GROUP, CDK32_SERVER_NAME);
		page.setName(getServerAdapter());
		dialog.next();
		NewCDK32ServerContainerWizardPage containerPage = new NewCDK32ServerContainerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		log.info("Setting hypervisor to: " + hypervisor);
		containerPage.setHypervisor(hypervisor);
		log.info("Setting binary to " + MINISHIFT_PROFILE);
		containerPage.setMinishiftBinary(MINISHIFT_PROFILE);
		// here comes possibility to set profile while creating server adapter
		log.info("Setting profile to: ");
		containerPage.setMinishiftProfile("");
		new WaitWhile(new SystemJobIsRunning(getJobMatcher(MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
	}
	
	@Test
	public void testLaunchConfiguration() {
		assertEquals(getServerAdapter(), launchDialog.getName());
		assertEquals(MINISHIFT_PROFILE, launchDialog.getLocation());
		assertTrue("Launch config arguments do not contains minishift profile" ,launchDialog.getArguments().getText().contains("--profile minishift"));
		assertTrue("Launch config arguments do not contains " + hypervisor,launchDialog.getArguments().getText().contains(hypervisor));
		assertEquals("Launh config env. variable does not have proper MINISHIFT_HOME", DEFAULT_MINISHIFT_HOME, launchDialog.getValueOfEnvVar("MINISHIFT_HOME"));
	}
	
	@Test
	public void testServerNamePropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		editor.getServernameLabel().setText(getServerAdapter() + "x");
		performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertEquals(getServerAdapter() + "x", launchDialog.getName());
	}
	
	@Test
	public void testProfilePropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getMinishiftProfile().setText("test");
		performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertTrue("Minishift profile was not changed to 'test'", launchDialog.getArguments().getText().contains("--profile test"));
	}
	
	@Test
	public void testLocationPropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getMinishiftBinaryLabel().setText(MOCK_CDK320);
		performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertEquals("Minishift location was not changed to " + MOCK_CDK320, MOCK_CDK320, launchDialog.getLocation());
	}
	
	@Test
	public void testMinishiftHomePropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		LabeledText home =  ((CDK32ServerEditor) editor).getMinishiftHomeLabel();
		String homePath = home.getText();
		home.setText(homePath + "Folder");
		performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertEquals(homePath + "Folder", launchDialog.getValueOfEnvVar("MINISHIFT_HOME"));
	}
	
	/**
	 * Open Launch configuration dialog via server editor and return object of CDKLaunchConfigurationDialog
	 * @return
	 */
	private void openLaunchConfiguration() {
		editor.openLaunchConfigurationFromLink();
		DefaultShell shell = new DefaultShell("Edit Configuration");
		shell.setFocus();
		launchDialog = new CDKLaunchConfigurationDialog(shell);
	}
	
	/**
	 * Close cdk launch configuration dialog if it is opened
	 */
	private void closeLaunchConfig() {
		if (launchDialog != null && launchDialog.isOpen()) {
			launchDialog.cancel();
			launchDialog = null;
		}
	}
}
