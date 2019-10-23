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
package org.jboss.tools.cdk.ui.bot.test.server.wizard;

import static org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils.assertSameMessage;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCRCServerWizardPage;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * CRC server wizard page UI test.
 * @author odockal
 *
 */
public class CRCServerWizardTest extends CDKServerWizardAbstractTest {

	private static String CRC_PATH;
	
	@BeforeClass
	public static void setupCDK3ServerEditorTest() {	
		if (CRC_BINARY == null) {	
			// nothing
		} else {	
			CRC_PATH = CRC_BINARY;	
		}
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_CRC;
	}
	
	@Test
	public void testCRCServerType() {
		assertServerType(CDKLabel.Server.CRC_SERVER_NAME, SERVER_ADAPTER_CRC);
	}
	
	@Test
	public void testNewCRCServerWizard() {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.CRC_SERVER_NAME);
		page.setName(getServerAdapter());
		dialog.next();
		NewCRCServerWizardPage containerPage = new NewCRCServerWizardPage(dialog);
		
		checkWizardPagewidget(CDKLabel.Labels.CRC_BINARY, CDKLabel.Server.CRC_SERVER_NAME);
		
		// needs to activate validator
		containerPage.setCRCBinary(NON_EXISTING_PATH);
		// Checking non existing path
		assertSameMessage(dialog, CDKLabel.Messages.DOES_NOT_EXIST);
		
		// existing directory
		containerPage.setCRCBinary(EXISTING_PATH);
		assertSameMessage(dialog, CDKLabel.Messages.NOT_FILE);
		
		// non executable file
		containerPage.setCRCBinary(NON_EXECUTABLE_FILE);
		assertSameMessage(dialog, CDKLabel.Messages.NOT_EXECUTABLE);
		// checking of crc binary validation
		// sets executable file - should be marked as error, requires proper crc binary
		// Will be covered by JBIDE-26878
		//containerPage.setCRCBinary(EXECUTABLE_FILE);
		//assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_IMAGE);
		
		// Positive test of proper minishift binary
		containerPage.setCRCBinary(CRC_PATH);
		containerPage.getCRCPullSecretFile().setText("");
		// validation of secret file
		assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_SECRET_FILE);
		// Validation of non existing path
		containerPage.setCRCPullServerFile(NON_EXISTING_PATH);
		assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_SECRET_FILE);
		// validation of directory
		containerPage.setCRCPullServerFile(EXISTING_PATH);
		assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_SECRET_FILE);	
		// validation of non executable file
		// Will be covered by JBIDE-26878
		//containerPage.setCRCPullServerFile(NON_EXECUTABLE_FILE);
		//assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_SECRET_FILE);
		//validation of invalid secret file (not a json)
		//containerPage.setCRCPullServerFile(EXECUTABLE_FILE);
		//assertSameMessage(dialog, CDKLabel.Messages.SELECT_VALID_SECRET_FILE);		
		// valid secret file
		containerPage.setCRCPullServerFile(CRC_SECRET_FILE);
		assertSameMessage(dialog, CDKLabel.Messages.SERVER_ADAPTER_REPRESENTING);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.MEDIUM, false);
		assertTrue("Expected Finish button is not enabled", dialog.isFinishEnabled());
		dialog.cancel();
	}
}
