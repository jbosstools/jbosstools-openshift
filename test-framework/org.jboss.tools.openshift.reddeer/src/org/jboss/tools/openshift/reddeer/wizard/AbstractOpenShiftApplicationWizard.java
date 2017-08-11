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
package org.jboss.tools.openshift.reddeer.wizard;

<<<<<<< HEAD
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.dialogs.NewWizard;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.combo.DefaultCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
=======
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.jface.viewer.handler.TreeViewerHandler;
import org.jboss.reddeer.swt.api.Combo;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.exception.SWTLayerException;
import org.jboss.reddeer.swt.impl.browser.InternalBrowser;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.YesButton;
import org.jboss.reddeer.swt.impl.combo.DefaultCombo;
import org.jboss.reddeer.swt.impl.menu.ShellMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.reddeer.workbench.impl.shell.WorkbenchShell;
>>>>>>> JBIDE-23131 Move openshift integration tests from
import org.jboss.tools.openshift.reddeer.condition.CentralIsLoaded;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Abstract wizard for a new OpenShift application.
 * 
 * @author mlabuda@redhat.com
 */
public abstract class AbstractOpenShiftApplicationWizard {
	
	protected TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();
	
	protected String server;
	protected String username;
	
	public AbstractOpenShiftApplicationWizard(String server, String username) {
		this.server = server;
		this.username = username;
	}
	
	/**
	 * Opens new application wizard via shell menu File - New. There has to be 
	 * an existing connection in OpenShift explorer, otherwise method fails.
	 */
	public void openWizardFromShellMenu() {
		new WorkbenchShell().setFocus();
		
		new NewWizard().open();
		
		new DefaultShell("New").setFocus();
		
		new DefaultTreeItem("OpenShift", "OpenShift Application").select();
		
		new NextButton().click();
		
		signToOpenShiftAndClickNext();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD).setFocus();
	}

	private void signToOpenShiftAndClickNext() {
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
		
		selectConnection(username, server, new DefaultCombo(0));
		
		new NextButton().click();
		processUntrustedSSLCertificate();
		
<<<<<<< HEAD
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
=======
		try {
			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			// Verify credentials job is still running, try to kill it and continue
			Job[] currentJobs = Job.getJobManager().find(null);
			for (Job job : currentJobs) {
				if (job.getName().startsWith("Verifying user credentials...")) {
					job.cancel();
				}
			}
			// close opened shell
			new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD).close();
		}
		new WaitUntil(new WidgetIsEnabled(new BackButton()), TimePeriod.LONG);
>>>>>>> JBIDE-23131 Move openshift integration tests from
	}

	private void processUntrustedSSLCertificate() {
		try{
<<<<<<< HEAD
			new WaitUntil(new ShellIsAvailable("Untrusted SSL Certificate"), TimePeriod.SHORT);
=======
			new DefaultShell("Untrusted SSL Certificate");
>>>>>>> JBIDE-23131 Move openshift integration tests from
			new YesButton().click();
		}catch (SWTLayerException ex){
			//do nothing SSL Certificate shell did not appear.
		}
	}

	private void selectConnection(String username, String server, Combo connectionCombo) {
		for (String comboItem: connectionCombo.getItems()) {
			if (comboItem.contains(username) 
					&& (server == null || comboItem.contains(server))) {
				connectionCombo.setSelection(comboItem);
				break;
			}
		}
	}
	
	/**
	 * Opens a new OpenShift application wizard from JBoss Central.
	 */
	public void openWizardFromCentral() {
		new DefaultToolItem(new WorkbenchShell(), OpenShiftLabel.Others.RED_HAT_CENTRAL).click();
		
		new WaitUntil(new CentralIsLoaded());
		
		new InternalBrowser().execute(OpenShiftLabel.Others.OPENSHIFT_CENTRAL_SCRIPT);
	
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
				TimePeriod.LONG);
		
		signToOpenShiftAndClickNext();
		
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD).setFocus();
	}

	protected void selectComboItem(String itemSubstring, DefaultCombo projectCombo) {
		for (String comboItem: projectCombo.getItems()) {
			if (comboItem.contains(itemSubstring)) {
				projectCombo.setSelection(comboItem);
				break;
			}
		}
	}

	/**
	 * Waits and clicks Back button.
	 */
	public void back() {
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
		
		new BackButton().click();
	}
	
	/**
	 * Waits and clicks Next button.
	 */
	public void next() {
		new WaitUntil(new ControlIsEnabled(new NextButton()), TimePeriod.LONG);
		
		new NextButton().click();
	}

	/**
	 * Waits and clicks Cancel button .
	 */
	public void cancel() {
		new WaitUntil(new ControlIsEnabled(new CancelButton()), TimePeriod.LONG);
		
		new CancelButton().click();
	}
	
	/**
	 * Waits and clicks Finish button.
	 */
	public void finish() {
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.LONG);
		
		new FinishButton().click();
	}
}
