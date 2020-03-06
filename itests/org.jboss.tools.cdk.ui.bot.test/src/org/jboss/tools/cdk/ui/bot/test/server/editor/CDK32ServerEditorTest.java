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
package org.jboss.tools.cdk.ui.bot.test.server.editor;

import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CredentialsPart;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for CDK 3.2+ server editor
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
public class CDK32ServerEditorTest extends CDKServerEditorAbstractTest {

	private String hypervisor = MINISHIFT_HYPERVISOR;
	
	private static final Logger log = Logger.getLogger(CDK32ServerEditorTest.class);

	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	private static String MINISHIFT_PATH;
	
	@BeforeClass
	public static void setupCDK32ServerEditorTest() {
		MINISHIFT_PATH = serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString();
	}
	
	@Before
	public void setup() {
		this.hypervisor = MINISHIFT_HYPERVISOR;
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
		containerPage.setMinishiftProfile(MINISHIFT_PROFILE);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
	}
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName(); 
	}

	public void setServerEditor() {
		serversView = new CDKServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CDK32ServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}
	
	@Test
	public void testCDK32ServerEditor() {
		assertCDKServerWizardFinished();
		setServerEditor();

		assertTrue(((CredentialsPart) editor).getUsernameLabel().getText().equalsIgnoreCase("minishift_username"));
		assertTrue(((CredentialsPart) editor).getPasswordLabel().getText().equalsIgnoreCase("minishift_password"));
		assertTrue(((CredentialsPart) editor).getDomainCombo().getSelection().equalsIgnoreCase(CDKLabel.Others.CREDENTIALS_DOMAIN));
		assertTrue(editor.getHostnameLabel().getText().equalsIgnoreCase(CDKLabel.Server.SERVER_HOST));
		assertTrue(
				((CDK32ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(MINISHIFT_HYPERVISOR));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftBinaryLabel().getText().equals(MINISHIFT_PATH));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftHomeLabel().getText().contains(".minishift"));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftProfile().getText().equals(MINISHIFT_PROFILE));
	}

	@Test
	public void testCDK32Hypervisor() {
		this.hypervisor = ANOTHER_HYPERVISOR;
		assertCDKServerWizardFinished();
		setServerEditor();

		assertTrue(
				((CDK32ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(ANOTHER_HYPERVISOR));
	}

	@Test
	public void testInvalidMinishiftLocation() {
		assertCDKServerWizardFinished();
		setServerEditor();

		checkEditorStateAfterSave(EXISTING_PATH, false);
		checkEditorStateAfterSave(NON_EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(NON_EXISTING_PATH, false);
		checkEditorStateAfterSave(EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(MOCK_CDK311, false);
		checkEditorStateAfterSave(MINISHIFT_PATH, true);
	}
	
}
