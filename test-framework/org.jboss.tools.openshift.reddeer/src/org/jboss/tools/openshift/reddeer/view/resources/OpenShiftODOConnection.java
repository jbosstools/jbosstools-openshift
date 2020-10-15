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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftODOProjectExists;
import org.jboss.tools.openshift.reddeer.dialogs.InputDialog;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;

/**
 * 
 * OpenShift Application Explorer Connection implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftODOConnection extends AbstractOpenShiftApplicationConnection {
	
	public OpenShiftODOConnection(TreeItem connectionItem) {
		super(connectionItem);
	}

	/**
	 * Gets an OpenShift project with specified project name.
	 *  
	 * @param projectName name of a project, if displayed name is provided,
	 *  	its displayed name, otherwise its project name
	 * @return OpenShift project
	 */
	public OpenShiftODOProject getProject(String projectName) {
		activateOpenShiftApplicationExplorerView();
		refresh();
		item.expand();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		return new OpenShiftODOProject(treeViewerHandler.getTreeItem(item, projectName));
		
	}

	/**
	 * Gets first project of a OpenShift connection.
	 *
	 * @return OpenShift project
	 */
	public OpenShiftODOProject getProject() {
		return getProject(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
	}
	
	/**
	 * Gets all projects existing on a connection.
	 * 
	 * @return list of all projects for a connection.
	 */
	public List<OpenShiftODOProject> getAllProjects() {
		List<OpenShiftODOProject> projects = new ArrayList<OpenShiftODOProject>();
		activateOpenShiftApplicationExplorerView();
		item.select();
		item.expand();
		for (TreeItem treeItem: item.getItems()) {
			if (treeItem.getText().contains("Click to login.")) {
				OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
				explorer.activate();
				explorer.connectToOpenShiftODO();
			}
			projects.add(new OpenShiftODOProject(treeItem));
		}
		return projects;
	}
	
	/**
	 * Creates a new OpenShift project for a connection.
	 * 
	 * @param projectName project name
	 * @param displayedName displayed name
	 * @return OpenShift Project
	 */
	public OpenShiftODOProject createNewProject(String projectName) {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_PROJECT).select();
		
		InputDialog dialog = new InputDialog(OpenShiftLabel.Shell.NEW_PROJECT);
		dialog.setInputText(projectName);
		dialog.ok();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(new Matcher[]{CoreMatchers.is(OpenShiftLabel.JobsLabels.CREATE_PROJECT)}), TimePeriod.LONG);
		
		new WaitUntil(new OpenShiftODOProjectExists(projectName), TimePeriod.LONG);
		return getProject(projectName);
	}

	public void refresh() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.REFRESH).select();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}

	/**
	 * Finds out whether a project with specified project name exists or not.
	 * 
	 * @param projectName project name
	 * @return true if project exists, false otherwise
	 */
	public boolean projectExists(String projectName) {
		try {
			getProject(projectName);
			return true;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	public void listCatalogServices() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.LIST_CATALOG_SERVICES).select();
	}

	public void listCatalogComponents() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.LIST_CATALOG_COMPONENTS).select();
	}

	public void openConsole() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.OPEN_CONSOLE).select();
	}
	
	public void about() {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.ABOUT).select();
	}

	public String getName() {
		return item.getText();
	}
}
