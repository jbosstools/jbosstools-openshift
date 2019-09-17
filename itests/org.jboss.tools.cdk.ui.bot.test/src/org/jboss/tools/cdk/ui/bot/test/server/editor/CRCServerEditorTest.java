/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
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
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CRCServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCRCServerWizardPage;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case covering CRC Server editor layout and behavior.
 * @author odockal
 *
 */
public class CRCServerEditorTest extends CDKServerEditorAbstractTest {

	private static final Logger log = Logger.getLogger(CRCServerEditorTest.class);
	
	private static String CRC_PATH;
	
	@BeforeClass
	public static void setupCRCerverEditorTest() {
		if (CRC_BINARY == null) {	
			// nothing yet
		} else {	
			CRC_PATH = CRC_BINARY;	
		}
	}
	
	@Override
	protected void setupServerWizardPage(NewMenuWizard dialog) {
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.CRC_SERVER_NAME);
		page.setName(getServerAdapter());
		if (!dialog.isNextEnabled()) {
			fail("Cannot create " + CDKLabel.Server.CRC_SERVER_NAME + " server adapter due to: " + dialog.getMessage());
		}
		dialog.next();
		NewCRCServerWizardPage containerPage = new NewCRCServerWizardPage(dialog);
		containerPage.setCRCBinary(CRC_PATH);
		// here comes possibility to set profile while creating server adapter
		log.info("Setting secret pull file to: ");
		containerPage.setCRCPullServerFile(CRC_SECRET_FILE);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_CRC;
	}

	public void setServerEditor() {
		serversView = new CDKServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CRCServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}
	
	@Test
	public void testCDK32ServerEditor() {
		assertCDKServerWizardFinished();
		setServerEditor();

		assertTrue(editor.getHostnameLabel().getText().equalsIgnoreCase(CDKLabel.Server.SERVER_HOST));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
		assertTrue(((CRCServerEditor) editor).getCRCBinary().getText().equals(CRC_PATH));
		assertTrue(((CRCServerEditor) editor).getCRCPullSecretFile().getText().contains(CRC_SECRET_FILE));
	}

	@Ignore
	@Test
	public void testInvalidCRCLocation() {
		assertCDKServerWizardFinished();
		setServerEditor();

		checkEditorStateAfterSave(EXISTING_PATH, false);
		checkEditorStateAfterSave(NON_EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(NON_EXISTING_PATH, false);
		checkEditorStateAfterSave(EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(CRC_PATH, true);
	}

}
