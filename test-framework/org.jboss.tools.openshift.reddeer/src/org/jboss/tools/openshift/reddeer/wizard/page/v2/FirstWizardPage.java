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
package org.jboss.tools.openshift.reddeer.wizard.page.v2;

import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.jface.viewer.handler.TreeViewerHandler;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.button.RadioButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * First wizard page of a New application wizard. 
 *  
 * @author mlabuda@redhat.com
 *
 */
public class FirstWizardPage {

	private TreeViewerHandler treeViewerHandler = TreeViewerHandler.getInstance();

	public FirstWizardPage() {
		new DefaultShell(OpenShiftLabel.Shell.NEW_APP_WIZARD);
	}
	
	/**
	 * Import existing application to the workspace.
	 * 
	 * @param domain domain name
	 * @param applicationName application name
	 */
	public void importExistingApplication(String domain, String applicationName) {
		if (!(new RadioButton(0).isSelected())) {
			new RadioButton(0).click();
		}
		
		new PushButton(OpenShiftLabel.Button.BROWSE).click();
		
		new DefaultShell("Select Existing Application");
		
		treeViewerHandler.getTreeItem(new DefaultTree(), domain, applicationName).select();
	
		new WaitUntil(new WidgetIsEnabled(new OkButton()), TimePeriod.NORMAL);
				
		new OkButton().click();
	}
	
	/**
	 * Create a new application on specified basic cartridge.
	 * 
	 * @param cartridge basic cartridge
	 */
	public void createNewApplicationOnBasicCartridge(String cartridge) {
		createNewApplication(true, cartridge);
	}
	
	/**
	 * Create a new application from specified quickstart.
	 * 
	 * @param quickstart quickstart
	 */
	public void createQuickstart(String quickstart) {
		createNewApplication(false, quickstart);
	}
	
	// basic cartridge is true in case of basic cartridge, false in case of quickstart
	// cartridge is either application platform cartridge or quickstart cartridge
	private void createNewApplication(boolean basicCartridge, String cartridge) {
		if (!(new RadioButton(1).isSelected())) {
			new RadioButton(1).click();
		}
		
		// basic cartridge or quickstart
		TreeItem category;
		if (basicCartridge) {
			category = new DefaultTreeItem("Basic Cartridges");
		} else {
			category = new DefaultTreeItem("Quickstarts");
		}
		
		category.select();
		category.expand();
		
		treeViewerHandler.getTreeItem(category, cartridge).select();
	}
	
	/**
	 * Create new application on a downloadable cartridge. 
	 * @param URL URL of downloadable cartridge
	 */
	public void createNewApplicationFromDownloadableCartridge(String URL) {
		if (!(new RadioButton(1).isSelected())) {
			new RadioButton(1).click();
		}
		
		treeViewerHandler.getTreeItem(new DefaultTree(), 
				OpenShiftLabel.Cartridge.DOWNLOADABLE_CARTRIDGE).select();
		
		new LabeledText("Cartridge URL:").setText(URL);
	}
}
