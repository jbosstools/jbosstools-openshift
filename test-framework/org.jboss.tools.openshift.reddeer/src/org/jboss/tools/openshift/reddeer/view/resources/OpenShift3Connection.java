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
package org.jboss.tools.openshift.reddeer.view.resources;

import java.util.ArrayList;
import java.util.List;

import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

public class OpenShift3Connection extends AbstractOpenShiftConnection {
	
	public OpenShift3Connection(TreeItem connectionItem) {
		super(connectionItem);
	}

	/**
	 * Gets an OpenShift 3 project with specified project name.
	 *  
	 * @param projectName name of a project, if displayed name is provided,
	 *  	its displayed name, otherwise its project name
	 * @return OpenShift project
	 */
	public OpenShiftProject getProject(String projectName) {
		activateOpenShiftExplorerView();
		item.expand();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		
		return new OpenShiftProject(treeViewerHandler.getTreeItem(item, projectName));
	}

	/**
	 * Gets first project of a OpenShift 3 connection.
	 *
	 * @return OpenShift project
	 */
	public OpenShiftProject getProject() {
		return getProject(DatastoreOS3.PROJECT1_DISPLAYED_NAME);
	}
	
	/**
	 * Gets all projects existing on a connection.
	 * 
	 * @return list of all projects for a connection.
	 */
	public List<OpenShiftProject> getAllProjects() {
		List<OpenShiftProject> projects = new ArrayList<OpenShiftProject>();
		activateOpenShiftExplorerView();
		item.select();
		item.expand();
		for (TreeItem treeItem: item.getItems()) {
			projects.add(new OpenShiftProject(treeItem));
		}
		return projects;
	}
	
	/**
	 * Creates a new OpenShift project for a connection based on first project name and project
	 * displayed name stored in {@link DatastoreOS3.PROJECT1}. The project name is generated every 
	 * time this method is called because of upstream issues.
	 * 
	 * @return OpenShift Project
	 */
	public OpenShiftProject createNewProject() {
		DatastoreOS3.generateProjectName();
		return createNewProject(DatastoreOS3.PROJECT1, DatastoreOS3.PROJECT1_DISPLAYED_NAME);
	}
	
	/**
	 * Creates a new OpenShift project for a connection based on second project name stored
	 * in {@link DatastoreOS3.PROJECT2}. The project name is generated every 
	 * time this method is called because of upstream issues.
	 * 
	 * @return OpenShift Project
	 */
	public OpenShiftProject createNewProject2() {
		DatastoreOS3.generateProject2Name();
		return createNewProject(DatastoreOS3.PROJECT2, null);
	}
	
	/**
	 * Creates a new OpenShift project for a connection.
	 * 
	 * @param projectName project name
	 * @param displayedName displayed name
	 * @return OpenShift Project
	 */
	public OpenShiftProject createNewProject(String projectName, String displayedName) {
		select();
		new ContextMenu(OpenShiftLabel.ContextMenu.NEW_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.CREATE_OS_PROJECT);
		new LabeledText(OpenShiftLabel.TextLabels.PROJECT_NAME).setText(projectName);
		if (displayedName != null) {
			new LabeledText(OpenShiftLabel.TextLabels.PROJECT_DISPLAYED_NAME).setText(displayedName);
		}
		new FinishButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.CREATE_OS_PROJECT), TimePeriod.LONG);
		
		if (displayedName == null) {
			return getProject(projectName);
		} else {
			return getProject(displayedName);
		}
	}
	
	/**
	 * Creates a new OpenShift Project for a connection.
	 * @param projectName project name
	 * @return OpenShift Project
	 */
	public OpenShiftProject createNewProject(String projectName) {
		return createNewProject(projectName, null);
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
}
