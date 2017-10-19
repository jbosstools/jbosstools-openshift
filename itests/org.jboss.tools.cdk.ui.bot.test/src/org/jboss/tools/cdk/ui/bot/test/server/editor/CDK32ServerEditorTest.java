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
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK32ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerContainerWizardPage;
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
public class CDK32ServerEditorTest extends CDKServerEditorAbstractTest {

	private String hypervisor = MINISHIFT_HYPERVISOR;
	
	private static final Logger log = Logger.getLogger(CDK32ServerEditorTest.class);

	@BeforeClass
	public static void setUpEnvironment() {
		checkMinishiftParameters();
		checkMinishiftProfileParameters();
	}
	
	@Before
	public void setup() {
		this.hypervisor = MINISHIFT_HYPERVISOR;
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
	
	@Override
	protected String getServerAdapter() {
		// return SERVER_ADAPTER_32; 
		// workaround for https://github.com/eclipse/reddeer/issues/1841
		return "Container Development Environment 3.2";
	}

	public void setServerEditor() {
		serversView = new CDEServersView();
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

		assertTrue(editor.getUsernameLabel().getText().equalsIgnoreCase("minishift_username"));
		assertTrue(editor.getPasswordLabel().getText().equalsIgnoreCase("minishift_password"));
		assertTrue(editor.getDomainCombo().getSelection().equalsIgnoreCase(CREDENTIALS_DOMAIN));
		assertTrue(editor.getHostnameLabel().getText().equalsIgnoreCase(SERVER_HOST));
		assertTrue(
				((CDK32ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(MINISHIFT_HYPERVISOR));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftBinaryLabel().getText().equals(MINISHIFT_PROFILE));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftHomeLabel().getText().contains(".minishift"));
		assertTrue(((CDK32ServerEditor) editor).getMinishiftProfile().getText().isEmpty());
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
		checkEditorStateAfterSave(MINISHIFT, false);
		checkEditorStateAfterSave(MINISHIFT_PROFILE, true);
	}
	
}
