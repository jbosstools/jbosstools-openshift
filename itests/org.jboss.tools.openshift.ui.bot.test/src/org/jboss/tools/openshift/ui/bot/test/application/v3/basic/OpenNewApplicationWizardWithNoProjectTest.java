/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.views.log.LogMessage;
import org.eclipse.reddeer.eclipse.ui.views.log.LogView;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.menu.ShellMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@CleanConnection
public class OpenNewApplicationWizardWithNoProjectTest extends AbstractTest {

	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	private String projectName;

	@Before
	public void generateProjectName() {
		projectName = "tmp-project" + System.currentTimeMillis();
	}
	
	@Test
	public void testOpenNewApplicationWizardViaShellMenuWithNoProjects() {
		new WorkbenchShell().setFocus();
		
		new ShellMenuItem("File", "New", "Other...").select();
		
		new DefaultShell("New").setFocus();
		
		new DefaultTreeItem("OpenShift", "OpenShift Application").select();
		
		new NextButton().click();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		
		for (String comboItem: new DefaultCombo(0).getItems()) {
			if (comboItem.contains(connectionReq.getUsername()) && comboItem.contains(connectionReq.getHost())) {
				new DefaultCombo(0).setSelection(comboItem);
				break;
			}
		}
		
		new NextButton().click();
		TestUtils.acceptSSLCertificate();		
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);

		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(projectName);
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		assertTrue("Created project was not preselected for a new OpenShift application",
				new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT).getText().equals(projectName));
		
		closeWizard();
	}
	
	@Test
	public void testOpenNewApplicationWizardFromCentralWithNoProjects() {
		new DefaultToolItem(new WorkbenchShell(), OpenShiftLabel.Others.RED_HAT_CENTRAL).click();
		
		new InternalBrowser().execute(OpenShiftLabel.Others.OPENSHIFT_CENTRAL_SCRIPT);
	
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
				TimePeriod.LONG);
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		
		for (String comboItem: new DefaultCombo(0).getItems()) {
			if (comboItem.contains(connectionReq.getUsername()) && comboItem.contains(connectionReq.getHost())) {
				new DefaultCombo(0).setSelection(comboItem);
				break;
			}
		}
		
		new NextButton().click();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(projectName);
		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD); 
		assertTrue("Created project was not preselected for a new OpenShift application",
				new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT).getText().equals(projectName));
		
		closeWizard();
	}
	
	@Test
	public void testOpenNewApplicationWizardFromOpenShiftExplorerWithNoProjects() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection = 
				explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_OS3_APPLICATION).select();
		
		try {
			new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Shell to create a new OpenShift application was supposed to be opened. But it's not.");
		}
		
		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(projectName);

		new FinishButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning());
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		
		assertTrue("Created project was not preselected for a new OpenShift application. Could be failing because "
				+ "of https://issues.jboss.org/browse/JBIDE-21593.",
				new LabeledCombo(OpenShiftLabel.TextLabels.PROJECT).getSelection().equals(projectName));
		
		closeWizard();
	}
	
	@Test
	public void testOpenNewApplicationWizardFromOpenShiftExplorerWithNoProjectsAndCancelNewProject() {
		clearLog();

		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		OpenShift3Connection connection = 
				explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_OS3_APPLICATION).select();;
		
		try {
			new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Shell to create a new OpenShift application was supposed to be opened. But it's not.");
		}
		
		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(projectName);
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		
		//do not click! New App wizard should close on canceling New Project wizard. 
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);

		checkErrorLog();

		clearLog();
	}

	private void closeWizard() {
		new CancelButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
	@After
	public void deleteTmpProject() {
		OpenShift3Connection connection = new OpenShiftExplorerView().getOpenShift3Connection(connectionReq.getConnection());
		connection.refresh();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		if (connection.projectExists(projectName)) {
			connection.getProject(projectName).delete();
		}
		
		//Delete connection too
		new OpenShiftExplorerView().getOpenShift3Connection(connectionReq.getConnection()).remove();
	}
	
	@AfterClass
	public static void createProjects() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.createNewProject();
		connection.createNewProject2();
	}

	private void clearLog() {
		LogView logView = new LogView();
		logView.open();
		logView.deleteLog();
	}

	private void checkErrorLog() {
		LogView logView = new LogView();
		logView.open();
		List<LogMessage> errorMessages = logView.getErrorMessages();
		assertEquals("There are errors in error log: "+errorMessages, 0, errorMessages.size());
	}
}
