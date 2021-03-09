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
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.ODOApplicationIsDeleted;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * OpenShift Application Explorer Project implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftODOApplication extends AbstractOpenShiftApplicationExplorerItem {
		
	private String projectName;
	private String applicationName;
	
	public OpenShiftODOApplication(TreeItem applicationItem, String projectName) {
		super(applicationItem);
		this.projectName = projectName;
		this.applicationName = treeViewerHandler.getNonStyledText(item);
	}
	
	public String getName() {
		return applicationName;
	}
	
	/**
	 * Deletes OpenShift project.
	 */
	public void delete() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_APPLICATION + " " + getName());
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(new Matcher[]{CoreMatchers.is(OpenShiftLabel.JobsLabels.DELETE)}), TimePeriod.LONG);
		new WaitWhile(new ODOApplicationIsDeleted(projectName, getName()), TimePeriod.getCustom(120));
	}
	
	public void openCreateComponentWizard() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_COMPONENT).select();
	}

	public void openCreateServiceWizard() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_SERVICE).select();
	}

  /**
   * @param componentName
   * @return
   */
	public OpenShiftODOComponent getComponent(String componentName) {
		select();
		item.expand();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		return new OpenShiftODOComponent(treeViewerHandler.getTreeItem(item, componentName), projectName, applicationName);
  }
}
