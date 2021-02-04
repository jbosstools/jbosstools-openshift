/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.ODOProjectIsDeleted;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * OpenShift Application Explorer Project implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftODOProject extends AbstractOpenShiftApplicationExplorerItem {
		
	private String projectName;
	
	public OpenShiftODOProject(TreeItem projectItem) {
		super(projectItem);
		this.projectName = treeViewerHandler.getNonStyledText(item);
	}
	
	public String getName() {
		return projectName;
	}
	
	/**
	 * Deletes OpenShift project.
	 */
	public void delete() {
		item.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_PROJECT + " " + getName());
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(new Matcher[]{CoreMatchers.is(OpenShiftLabel.JobsLabels.DELETE)}), TimePeriod.LONG);
		new WaitUntil(new ODOProjectIsDeleted(getName()), TimePeriod.getCustom(120));
	}
	
	public void openCreateComponentWizard() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_COMPONENT).select();
	}

	public void openCreateServiceWizard() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_SERVICE).select();
	}

	public OpenShiftODOApplication getApplication(String applicationName) {
		activateOpenShiftApplicationExplorerView();
		item.expand();
    
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		return new OpenShiftODOApplication(treeViewerHandler.getTreeItem(item, applicationName), projectName);
	}
}
