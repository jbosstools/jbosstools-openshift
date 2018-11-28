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
package org.jboss.tools.cdk.ui.bot.test.server.wizard;

import static org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils.assertDiffMessage;
import static org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils.assertSameMessage;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK32ServerWizardPage;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Class for testing CDK 3.2 server wizard functionality
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK32ServerWizardTest extends CDKServerWizardAbstractTest {
	
	private static String MINISHIFT_PATH;
	
	@BeforeClass
	public static void setupCDK3ServerEditorTest() {	
		if (CDK32_MINISHIFT == null) {	
			MINISHIFT_PATH = MOCK_CDK320;	
		} else {	
			MINISHIFT_PATH = CDK32_MINISHIFT;	
		}
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@Test
	public void testCDK32ServerType() {
		assertServerType(CDKLabel.Server.CDK32_SERVER_NAME);
	}
	
	@Test
	public void testNewCDK32ServerWizard() {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.CDK32_SERVER_NAME);
		page.setName(getServerAdapter());
		dialog.next();
		NewCDK32ServerWizardPage containerPage = new NewCDK32ServerWizardPage(dialog);
		
		checkWizardPagewidget("Minishift Binary: ", CDKLabel.Server.CDK32_SERVER_NAME);

		// just check that default domain is choosen correctly
		assertTrue(containerPage.getDomain().equalsIgnoreCase(CDKLabel.Others.CREDENTIALS_DOMAIN));
		assertTrue(containerPage.getMinishiftProfile().getText().equals("minishift"));
		
		// needs to activate validator
		containerPage.setMinishiftBinary(EXISTING_PATH);
		
		// first the credentials are checked
		assertSameMessage(dialog, CDKLabel.Messages.NO_USER);
		containerPage.setCredentials(USERNAME, PASSWORD);
		assertDiffMessage(dialog, CDKLabel.Messages.NO_USER);

		// checking of minishift binary validation
		// test that existing folder cannot be run
		containerPage.setMinishiftBinary(EXISTING_PATH);
		assertSameMessage(dialog, CDKLabel.Messages.CANNOT_RUN_PROGRAM);
		containerPage.setMinishiftBinary(NON_EXECUTABLE_FILE);
		assertSameMessage(dialog, CDKLabel.Messages.NOT_EXECUTABLE);
		containerPage.setMinishiftBinary(NON_EXISTING_PATH);
		assertSameMessage(dialog, CDKLabel.Messages.DOES_NOT_EXIST);
		containerPage.setMinishiftBinary(EXECUTABLE_FILE);
		assertSameMessage(dialog, CDKLabel.Messages.CHECK_MINISHIFT_VERSION);
		
		// check compatibility of cdk version with server adapter
		containerPage.setMinishiftBinary(MOCK_CDK311);
		assertSameMessage(dialog, CDKLabel.Messages.NOT_COMPATIBLE);
		
		// Positive test of proper minishift binary
		containerPage.setMinishiftBinary(MINISHIFT_PATH);
		assertDiffMessage(dialog, CDKLabel.Messages.CHECK_MINISHIFT_VERSION);
		assertSameMessage(dialog, CDKLabel.Messages.SERVER_ADAPTER_REPRESENTING);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.MEDIUM, false);
		assertTrue("Expected Finish button is not enabled", dialog.isFinishEnabled());
		dialog.cancel();
	}	

}
