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
import org.jboss.tools.openshift.reddeer.condition.ODOComponentIsDeleted;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.widget.terminal.TerminalHasNoChange;

/**
 * 
 * OpenShift Application Explorer Project implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftODOComponent extends AbstractOpenShiftApplicationExplorerItem {
		
	private String projectName;
	private String applicationName;
	private String componentName;
	
	public OpenShiftODOComponent(TreeItem applicationItem, String projectName, String applicationName) {
		super(applicationItem);
		this.projectName = projectName;
		this.applicationName = applicationName;
		this.componentName = treeViewerHandler.getNonStyledText(item);
		item.select();
	}
	
	public String getName() {
		return componentName;
	}
	
	/**
	 * Push OpenShift component.
	 */
	public void push() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.PUSH).select();
    
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitWhile(new TerminalHasNoChange(), TimePeriod.VERY_LONG);
	}
  
	/**
	 * Debug OpenShift component.
	 */
	public void debug() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DEBUG).select();
    
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		new WaitWhile(new TerminalHasNoChange(), TimePeriod.VERY_LONG, false);
	}

	/**
	 * Deletes OpenShift component.
	 */
	public void delete() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_COMPONENT + " " + getName());
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(new Matcher[]{CoreMatchers.is(OpenShiftLabel.JobsLabels.DELETE)}), TimePeriod.LONG);
		new WaitWhile(new ODOComponentIsDeleted(projectName, applicationName, getName()), TimePeriod.getCustom(120));
	}
	
	public void openCreateURLWizard() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_URL).select();
	}

}
