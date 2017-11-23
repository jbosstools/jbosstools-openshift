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

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
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
import org.jboss.tools.openshift.core.connection.Connection;
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
	protected Connection connection;
	
	public AbstractOpenShiftApplicationWizard(String server, String username) {
		this.server = server;
		this.username = username;
	}
	
	public AbstractOpenShiftApplicationWizard(Connection connection) {
		this.connection = connection;
		this.server = connection.getHost();
		this.username = connection.getUsername();
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
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ControlIsEnabled(new BackButton()), TimePeriod.LONG);
	}

	private void processUntrustedSSLCertificate() {
		try{
			new DefaultShell("Untrusted SSL Certificate");
			new YesButton().click();
		}catch (CoreLayerException ex){
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
