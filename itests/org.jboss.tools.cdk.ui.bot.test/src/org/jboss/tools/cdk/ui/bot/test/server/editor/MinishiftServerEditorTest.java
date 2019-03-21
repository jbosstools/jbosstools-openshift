/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
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
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDKPart;
import org.jboss.tools.cdk.reddeer.server.ui.editor.Minishift17ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.MinishiftServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewMinishiftServerWizardPage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Minishift's server editor tests
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
@ContainerRuntimeServer(
		version = CDKVersion.MINISHIFT1320,
		useExistingBinaryFromConfig=true,
		makeRuntimePersistent=true,
		createServerAdapter=false,
		useExistingBinaryInProperty="minishift")
public class MinishiftServerEditorTest extends CDKServerEditorAbstractTest {

	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;

	private String hypervisor = MINISHIFT_HYPERVISOR;
	
	private static final Logger log = Logger.getLogger(MinishiftServerEditorTest.class);
	
	private static String MINISHIFT_PATH;
	
	@BeforeClass
	public static void setupCDK3ServerEditorTest() {
		MINISHIFT_PATH = serverRequirement.getServerAdapter().getMinishiftBinary().toAbsolutePath().toString();
	}
	
	@Override
	protected String getServerAdapter() {
		return serverRequirement.getServerAdapter().getAdapterName();
	}
	
	@Before
	public void setup() {
		this.hypervisor = MINISHIFT_HYPERVISOR;
	}
	
	@Override
	public void setServerEditor() {
		serversView = new CDKServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new Minishift17ServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}

	@Override
	protected void setupServerWizardPage(NewMenuWizard dialog) {
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.MINISHIFT_SERVER_NAME);
		page.setName(SERVER_ADAPTER_MINISHIFT);
		dialog.next();
		NewMinishiftServerWizardPage containerPage = new NewMinishiftServerWizardPage(dialog);
		log.info("Setting hypervisor to: " + hypervisor);
		containerPage.setHypervisor(hypervisor);
		log.info("Setting binary to " + MINISHIFT_PATH);
		containerPage.setMinishiftBinary(MINISHIFT_PATH);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
	}

	@Test
	public void testMinishiftServerEditor() {
		assertCDKServerWizardFinished();
		setServerEditor();

		assertTrue(((MinishiftServerEditor)editor).getHostnameLabel().getText().equalsIgnoreCase(CDKLabel.Server.SERVER_HOST));
		assertTrue(
				((Minishift17ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(MINISHIFT_HYPERVISOR));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
		assertTrue(((Minishift17ServerEditor) editor).getMinishiftBinaryLabel().getText().equals(MINISHIFT_PATH));
		assertTrue(((Minishift17ServerEditor) editor).getMinishiftHomeLabel().getText().contains(".minishift"));
		assertTrue(((Minishift17ServerEditor) editor).getMinishiftProfile().getText().equals("minishift"));
	}

	@Test
	public void testMinishiftHypervisor() {
		this.hypervisor = ANOTHER_HYPERVISOR;
		assertCDKServerWizardFinished();
		setServerEditor();

		assertTrue(
				((CDKPart) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(ANOTHER_HYPERVISOR));
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
		checkEditorStateAfterSave(MOCK_CDK320, false);
		checkEditorStateAfterSave(MOCK_MINISHIFT131, false);
		checkEditorStateAfterSave(MINISHIFT_PATH, true);
	}

}
