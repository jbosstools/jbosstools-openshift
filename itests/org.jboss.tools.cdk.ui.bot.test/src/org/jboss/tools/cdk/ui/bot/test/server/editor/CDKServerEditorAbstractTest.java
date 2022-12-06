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

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.selectionwizard.NewMenuWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.cdk.reddeer.core.condition.SystemJobIsRunning;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.server.ui.editor.MinishiftServerEditor;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDKServerWizardAbstractTest;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.junit.After;

/**
 * Abstract test class for CDK 3.2+ server editor 
 * @author odockal
 *
 */
public abstract class CDKServerEditorAbstractTest extends CDKServerWizardAbstractTest {
	
	protected ServersView2 serversView;

	protected MinishiftServerEditor editor;

	protected static final String ANOTHER_HYPERVISOR = "virtualbox";
	
	private static Logger log = Logger.getLogger(CDKServerEditorAbstractTest.class);

	public abstract void setServerEditor();
	
	@After
	public void tearDownServerAbstract() {
		cleanUp();
	}
	
	protected abstract void setupServerWizardPage(NewMenuWizard dialog);
	
	public void cleanUp() {
		if (editor != null) {
			if (!editor.isActive()) {
				editor.activate();
			}
			editor.close(false);
			editor = null;
		}
		if (serversView != null) {
			if (serversView.isOpen()) {
				serversView.close();
				serversView = null;
			}
		}
	}

	public void addCDKServer() {
		NewCDKServerWizard dialog = CDKUtils.openNewServerWizardDialog();
		
		try {
			setupServerWizardPage(dialog);
			new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.DEFAULT, false);
			dialog.finish(TimePeriod.DEFAULT);
		} catch (RedDeerException coreExc) {
			new CancelButton().click();
			throw new CDKServerException("Exception occured in CDK server wizard, wizard was canceled", coreExc);
		}
	}
	
	protected void assertCDKServerWizardFinished() {
		try {
			addCDKServer();
		} catch (CDKServerException exc) {
			log.error(exc.getMessage());
			fail("Fails to create " + getServerAdapter() + " Server via New Server Wizard due to " + exc.getCause().getMessage());
		}
	}
	
	protected void checkEditorStateAfterSave(String location, boolean canSave) {
		LabeledText label = editor.getBinaryLabel();
		label.setText(location);
		new WaitUntil(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.SHORT, false);
		new WaitWhile(new SystemJobIsRunning(new JobMatcher(CDKLabel.Job.MINISHIFT_VALIDATION_JOB)), TimePeriod.DEFAULT, false);
		if (canSave) {
			verifyEditorCanSave(location);
		} else {
			verifyEditorCannotSave(location);
		}
	}

	private void verifyEditorCannotSave(String location) {
		assertTrue(editor.isDirty());
		try {
			CDKTestUtils.performSave(editor.getEditorPart());
			new WaitWhile(new JobIsRunning());
			fail("Editor was saved successfully while passing " + location + " but exception was expected");
		} catch (WaitTimeoutExpiredException exc) {
			log.info("WaitTimeoutExpiredException occured, editor was not saved as expected");
		}
		errorDialogAppeared("Error Message Dialog did not appear while trying to save editor after passing " + location);
		assertTrue(editor.isDirty());
	}

	private void verifyEditorCanSave(String location) {
		assertTrue(editor.isDirty());
		try {
			CDKTestUtils.performSave(editor.getEditorPart());
			log.info("Editor was saved as expected");
		} catch (WaitTimeoutExpiredException exc) {
			fail("Editor was not saved successfully and exception was thrown while passing " + location);
		}
		assertFalse(editor.isDirty());
	}

	private void errorDialogAppeared(String failMessage) {
		try {
			new WaitUntil(new ShellIsAvailable(new DefaultShell(getServerAdapter())), TimePeriod.MEDIUM);
		} catch (WaitTimeoutExpiredException exc) {
			log.error(exc.getMessage());
			fail(failMessage);
		}
		new OkButton().click();
	}
	
}
