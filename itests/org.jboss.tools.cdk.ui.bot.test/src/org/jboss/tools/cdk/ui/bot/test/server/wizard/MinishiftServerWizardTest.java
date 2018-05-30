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
package org.jboss.tools.cdk.ui.bot.test.server.wizard;

import static org.junit.Assert.assertTrue;
import static org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils.assertDiffMessage;
import static org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils.assertSameMessage;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewMinishiftServerWizardPage;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Minishift's server wizard tests
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class MinishiftServerWizardTest extends CDKServerWizardAbstractTest {

	private static String MINISHIFT_PATH;
	
	@BeforeClass
	public static void setUpEnvironment() {
		if (CDK_MINISHIFT == null) {
			MINISHIFT_PATH = MOCK_MINISHIFT170;
		} else {
			MINISHIFT_PATH = MINISHIFT;
		}
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_MINISHIFT;
	}
	
	@Test
	public void testMinishiftServerType() {
		assertServerType(CDKLabel.Server.MINISHIFT_SERVER_NAME);
	}
	
	@Test
	public void testNewMinishiftServerWizard() {
		NewCDKServerWizard dialog = (NewCDKServerWizard)CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		
		page.selectType(CDKLabel.Server.SERVER_TYPE_GROUP, CDKLabel.Server.MINISHIFT_SERVER_NAME);
		page.setName(getServerAdapter());
		dialog.next();
		NewMinishiftServerWizardPage containerPage = new NewMinishiftServerWizardPage();
		
		checkWizardPagewidget("Minishift Binary: ", CDKLabel.Server.MINISHIFT_SERVER_NAME);
		assertTrue(containerPage.getMinishiftProfile().getText().contains("minishift"));
		
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
		
		// check compatibility of cdk version with server adapter
		containerPage.setMinishiftBinary(MOCK_CDK320);
		assertSameMessage(dialog, CDKLabel.Messages.NOT_COMPATIBLE);

		// check compatibility of cdk version with server adapter
		containerPage.setMinishiftBinary(MOCK_MINISHIFT131);
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
