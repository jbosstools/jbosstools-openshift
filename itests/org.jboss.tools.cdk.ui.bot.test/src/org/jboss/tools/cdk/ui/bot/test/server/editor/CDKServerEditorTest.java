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
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDKServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerContainerWizardPage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for CDK 2.x server editor page
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDKServerEditorTest extends CDKServerEditorAbstractTest {
	
	private static final Logger log = Logger.getLogger(CDKServerEditorTest.class);
	
	@BeforeClass
	public static void setup() {
		checkVagrantfileParameters();
	}


	@Override
	protected void setupServerWizardPage(NewMenuWizard dialog) {
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		
		page.selectType(SERVER_TYPE_GROUP, CDK_SERVER_NAME);
		page.setName(SERVER_ADAPTER);
		dialog.next();
		NewCDKServerContainerWizardPage containerPage = new NewCDKServerContainerWizardPage();
		containerPage.setCredentials(USERNAME, PASSWORD);
		log.info("Setting vagrantfile path to: " + VAGRANTFILE);
		containerPage.setFolder(VAGRANTFILE);
		new WaitWhile(new SystemJobIsRunning(getJobMatcher(MINISHIFT_VALIDATION_JOB)), TimePeriod.MEDIUM, false);	
	}
	
	@Override
	public void setServerEditor() {
		serversView = new CDEServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CDKServerEditor(getServerAdapter());
		editor.activate();
	}
	
	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER;
	}
	
	@Test
	public void testCDKServerEditor() {
		assertCDKServerWizardFinished();
		setServerEditor();
		assertTrue(editor.getUsernameLabel().getText().equalsIgnoreCase("sub_username"));
		assertTrue(editor.getPasswordLabel().getText().equalsIgnoreCase("sub_password"));
		assertTrue(editor.getDomainCombo().getSelection().equalsIgnoreCase(CREDENTIALS_DOMAIN));
		assertTrue(editor.getHostnameLabel().getText().equalsIgnoreCase(SERVER_HOST));
		assertTrue(((CDKServerEditor)editor).getVagrantfileLocation().getText().equals(VAGRANTFILE));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
	}

}
