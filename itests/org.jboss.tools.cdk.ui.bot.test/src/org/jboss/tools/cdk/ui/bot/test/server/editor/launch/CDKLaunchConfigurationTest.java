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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.launch.configuration.CDKLaunchConfigurationDialog;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
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
@ContainerRuntimeServer(
		version = CDKVersion.CDK3120,
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		createServerAdapter=false,
		useExistingBinaryInProperty="cdk32.minishift")
public class CDKLaunchConfigurationTest extends CDKServerEditorAbstractTest {
	
	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	private static String MINISHIFT_PATH;
	
	private String hypervisor = MINISHIFT_HYPERVISOR;
	
	private CDKLaunchConfigurationDialog launchDialog; 
	
	private static final Logger log = Logger.getLogger(CDKLaunchConfigurationTest.class);
	
	@BeforeClass
	public static void setUpEnvironment() {
		log.info("Checking given program arguments"); 
		checkDevelopersParameters();
		MINISHIFT_PATH = serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString();
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
		CDKTestUtils.removeAccessRedHatCredentials(CDKLabel.Others.CREDENTIALS_DOMAIN, USERNAME);
	}
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}

	@Override
	public void setServerEditor() {
		serversView = new CDKServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CDK32ServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}

	@Override
	protected void setupServerWizardPage(NewMenuWizard dialog) {
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.CDK32_SERVER_NAME);
		page.setName(getServerAdapter());
		dialog.next();
		NewCDK32ServerWizardPage containerPage = new NewCDK32ServerWizardPage(dialog);
		containerPage.setCredentials(USERNAME, PASSWORD);
		log.info("Setting hypervisor to: " + hypervisor);
		containerPage.setHypervisor(hypervisor);
		log.info("Setting binary to " + MINISHIFT_PATH);
		containerPage.setMinishiftBinary(MINISHIFT_PATH);
		// here comes possibility to set profile while creating server adapter
		log.info("Setting profile to: ");
		containerPage.setMinishiftProfile("");
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
	}
	
	@Test
	public void testLaunchConfiguration() {
		assertEquals(getServerAdapter(), launchDialog.getName());
		assertEquals(MINISHIFT_PATH, launchDialog.getLocation());
		assertTrue("Launch config arguments do not contains minishift profile" ,launchDialog.getArguments().getText().contains("--profile minishift"));
		assertTrue("Launch config arguments do not contains " + hypervisor,launchDialog.getArguments().getText().contains(hypervisor));
		assertEquals("Launh config env. variable does not have proper MINISHIFT_HOME", DEFAULT_MINISHIFT_HOME, launchDialog.getValueOfEnvVar("MINISHIFT_HOME"));
	}
	
	@Test
	public void testServerNamePropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		editor.getServernameLabel().setText(getServerAdapter() + "x");
		CDKTestUtils.performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertEquals(getServerAdapter() + "x", launchDialog.getName());
	}
	
	@Test
	public void testProfilePropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getMinishiftProfile().setText("test");
		CDKTestUtils.performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertTrue("Minishift profile was not changed to 'test'", launchDialog.getArguments().getText().contains("--profile test"));
	}
	
	@Test
	public void testLocationPropagationIntoLaunchConfig() {
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getMinishiftBinaryLabel().setText(MOCK_CDK320);
		CDKTestUtils.performSave(editor.getEditorPart());
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
		CDKTestUtils.performSave(editor.getEditorPart());
		openLaunchConfiguration();
		assertEquals(homePath + "Folder", launchDialog.getValueOfEnvVar("MINISHIFT_HOME"));
	}
	
	/**
	 * Covers JBIDE-25617
	 */
	@Test
	public void testSkipRegistrationFlagPropagation() {
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getAddSkipRegistrationOnStartCheckBox().toggle(true);
		CDKTestUtils.performSave(editor.getEditorPart());
		openLaunchConfiguration();
		String arguments = launchDialog.getArguments().getText();
		assertTrue("Launch config arguments are missing " + SKIP_REGISTRATION + " flag, arguments: " + arguments,
				arguments.contains(SKIP_REGISTRATION));
		closeLaunchConfig();
		editor.activate();
		((CDK32ServerEditor) editor).getAddSkipRegistrationOnStartCheckBox().toggle(false);
		CDKTestUtils.performSave(editor.getEditorPart());
		openLaunchConfiguration();
		arguments = launchDialog.getArguments().getText();
		assertFalse("Launch config arguments should not contain " + SKIP_REGISTRATION + " flag, arguments: " + arguments,
				arguments.contains(SKIP_REGISTRATION));
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
