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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.EditorHandler;
import org.eclipse.ui.IEditorPart;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDEServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.editor.CDK3ServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDK3ServerContainerWizardPage;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDKServerWizardAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Class tests CDK3 server editor page
 * 
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
public class CDK3ServerEditorTest extends CDKServerWizardAbstractTest {

	private ServersView2 serversView;

	private CDEServerEditor editor;

	private static final String ANOTHER_HYPERVISOR = "virtualbox";

	private static Logger log = Logger.getLogger(CDK3ServerEditorTest.class);

	private void setServerEditor() {
		serversView = new CDEServersView();
		serversView.open();
		serversView.getServer(getServerAdapter()).open();
		editor = new CDK3ServerEditor(getServerAdapter());
		editor.activate();
		new WaitUntil(new JobIsRunning(), TimePeriod.getCustom(1), false);
	}

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_3;
	}
	
	@After
	public void tearDown() {
		cleanUp();
	}

	@Test
	public void testCDK3ServerEditor() {
		assertCDK3ServerWizardFinished(MINISHIFT_HYPERVISOR, MINISHIFT_PATH);
		setServerEditor();

		assertTrue(editor.getUsernameLabel().getText().equalsIgnoreCase("minishift_username"));
		assertTrue(editor.getPasswordLabel().getText().equalsIgnoreCase("minishift_password"));
		assertTrue(editor.getDomainCombo().getSelection().equalsIgnoreCase(CREDENTIALS_DOMAIN));
		assertTrue(editor.getHostnameLabel().getText().equalsIgnoreCase(SERVER_HOST));
		assertTrue(
				((CDK3ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(MINISHIFT_HYPERVISOR));
		assertTrue(editor.getServernameLabel().getText().equals(getServerAdapter()));
		assertTrue(((CDK3ServerEditor) editor).getMinishiftBinaryLabel().getText().equals(MINISHIFT_PATH));
		assertTrue(((CDK3ServerEditor) editor).getMinishiftHomeLabel().getText().contains(".minishift"));
	}

	@Test
	public void testCDK3Hypervisor() {
		assertCDK3ServerWizardFinished(ANOTHER_HYPERVISOR, MINISHIFT_PATH);
		setServerEditor();

		assertTrue(
				((CDK3ServerEditor) editor).getHypervisorCombo().getSelection().equalsIgnoreCase(ANOTHER_HYPERVISOR));
	}

	@Test
	public void testInvalidMinishiftLocation() {
		assertCDK3ServerWizardFinished(MINISHIFT_HYPERVISOR, MINISHIFT_PATH);
		setServerEditor();

		checkEditorStateAfterSave(EXISTING_PATH, false);
		checkEditorStateAfterSave(NON_EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(NON_EXISTING_PATH, false);
		checkEditorStateAfterSave(EXECUTABLE_FILE, false);
		checkEditorStateAfterSave(MINISHIFT_PATH, true);
	}

	private void cleanUp() {
		if (editor != null) {
			if (!editor.isActive()) {
				editor.activate();
			}
			editor.save();
			editor.close();
			editor = null;
		}
		if (serversView.isOpen()) {
			serversView.close();
			serversView = null;
		}
	}

	private void checkEditorStateAfterSave(String location, boolean canSave) {
		LabeledText label = ((CDK3ServerEditor) editor).getMinishiftBinaryLabel();
		label.setText(location);
		new WaitUntil(new SystemJobIsRunning(getJobMatcher(MINISHIFT_VALIDATION_JOB)), TimePeriod.SHORT, false);
		new WaitWhile(new SystemJobIsRunning(getJobMatcher(MINISHIFT_VALIDATION_JOB)), TimePeriod.DEFAULT, false);
		if (canSave) {
			verifyEditorCanSave();
		} else {
			verifyEditorCannotSave();
		}
	}

	/**
	 * We need to override save method from EditorHandler to be executed in async
	 * thread in order to be able to work with message dialog from invalid server
	 * editor location
	 * 
	 * @param editor
	 *            IEditorPart to work with during saving
	 */
	private void performSave(final IEditorPart editor) {
		EditorHandler.getInstance().activate(editor);
		Display.asyncExec(new Runnable() {

			@Override
			public void run() {
				editor.doSave(new NullProgressMonitor());

			}
		});
		new WaitUntil(new WaitCondition() {

			@Override
			public boolean test() {
				return !editor.isDirty();
			}

			@Override
			public String description() {
				return " editor is not dirty...";
			}

			@Override
			public <T> T getResult() {
				return null;
			}

			@Override
			public String errorMessageWhile() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String errorMessageUntil() {
				// TODO Auto-generated method stub
				return null;
			}
		}, TimePeriod.MEDIUM);
	}

	private void verifyEditorCannotSave() {
		assertTrue(editor.isDirty());
		try {
			performSave(editor.getEditorPart());
			new WaitWhile(new JobIsRunning());
			fail("Editor was saved successfully but exception was expected");
		} catch (WaitTimeoutExpiredException exc) {
			log.info("WaitTimeoutExpiredException occured, editor was not saved as expected");
		}
		errorDialogAppeared();
		assertTrue(editor.isDirty());
	}

	private void verifyEditorCanSave() {
		assertTrue(editor.isDirty());
		try {
			performSave(editor.getEditorPart());
			log.info("Editor was saved as expected");
		} catch (WaitTimeoutExpiredException exc) {
			fail("Editor was not saved successfully but exception was thrown");
		}
		assertFalse(editor.isDirty());
	}

	private void errorDialogAppeared() {
		try {
			new WaitUntil(new ShellIsAvailable(new DefaultShell(getServerAdapter())), TimePeriod.MEDIUM);
			log.info("Error Message Dialog appeared as expected");
		} catch (WaitTimeoutExpiredException exc) {
			log.error(exc.getMessage());
			fail("Error Message Dialog did not appear while trying to save editor");
		}
		new OkButton().click();
	}

	private static void addCDK3Server(String hypervisor, String binary) {
		NewServerWizard dialog = CDKTestUtils.openNewServerWizardDialog();
		NewServerWizardPage page = new NewServerWizardPage(dialog);
		
		try {
			page.selectType(SERVER_TYPE_GROUP, CDK3_SERVER_NAME);
			page.setName(SERVER_ADAPTER_3);
			dialog.next();
			NewCDK3ServerContainerWizardPage containerPage = new NewCDK3ServerContainerWizardPage();
			containerPage.setCredentials(USERNAME, PASSWORD);
			log.info("Setting hypervisor");
			containerPage.setHypervisor(hypervisor);
			log.info("Setting binary");
			containerPage.setMinishiftBinary(binary);
			if (!dialog.isFinishEnabled()) {
				new WaitUntil(new JobIsRunning(), TimePeriod.SHORT, false);
			}
			dialog.finish(TimePeriod.MEDIUM);
		} catch (CoreLayerException coreExc) {
			new CancelButton().click();
			throw new CDKServerException("Exception occured in CDK server wizard, wizard was canceled", coreExc);
		}
	}
	
	protected void assertCDK3ServerWizardFinished(String hypervisor, String binary) {
		try {
			addCDK3Server(hypervisor, binary);
		} catch (CDKServerException exc) {
			exc.printStackTrace();
			fail("Fails to create CDK3 Server via New Server Wizard due to " + exc.getMessage());
		}
	}
	
}