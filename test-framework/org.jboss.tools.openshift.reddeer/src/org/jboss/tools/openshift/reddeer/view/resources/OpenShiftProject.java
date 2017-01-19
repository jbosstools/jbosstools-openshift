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

import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesView;
import org.jboss.reddeer.eclipse.ui.views.properties.TabbedPropertyList;
import org.jboss.reddeer.swt.api.TableItem;
import org.jboss.reddeer.swt.api.ToolItem;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

public class OpenShiftProject extends AbstractOpenShiftExplorerItem {
		
	private String projectName;
	
	public OpenShiftProject(TreeItem projectItem) {
		super(projectItem);
		projectName = treeViewerHandler.getNonStyledText(item);
	}
	
	public String getName() {
		return projectName;
	}

	/**
	 * Gets an OpenShift service with specified name.
	 * 
	 * @param name name of an OpenShift service
	 * @return OpenShift service
	 */
	public Service getService(String name) {
		return new Service(treeViewerHandler.getTreeItem(item, name));
	}
	
	/**
	 * Gets OpenShift resource of specified type matching specified name.
	 * 
	 * @param resourceType type of OpenShift resource
	 * @param name name of OpenShift resource
	 * @return OpenShift resource
	 */
	public OpenShiftResource getOpenShiftResource(Resource resourceType, String name) {
		List<OpenShiftResource> resources = getOpenShiftResources(resourceType);
		if (!resources.isEmpty()) {
			for (OpenShiftResource resource: resources) {
				if (resource.getName().equals(name)) {
					return resource;
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets all resources of specific type for project.
	 * 
	 * @param resourceType resource type
	 * @return list of resources of specified type or empty list if there are no resources
	 */
	public List<OpenShiftResource> getOpenShiftResources(Resource resourceType) {
		return getOpenShiftResources(resourceType, false);
	}
		
	/**
	 * Gets all resources of specific type for project and allows to lock properties view 
	 * @param resourceType resource type
	 * @param pinView true to lock properties view, false otherwise
	 * @return list of resources of specified type or empty list if there are no resources
	 */
	public List<OpenShiftResource> getOpenShiftResources(Resource resourceType, boolean pinView) {
		List<OpenShiftResource> resources = new ArrayList<OpenShiftResource>();
		expand();
		openProperties();
		
		togglePinPropertiesView(pinView);
		
		selectTabbedProperty("Details");
		selectTabbedProperty(resourceType.toString());
		List<TableItem> tableItems = new DefaultTable().getItems();
		if (!tableItems.isEmpty()) {
			for (TableItem tableItem: tableItems) {
				resources.add(new OpenShiftResource(tableItem));
			}
		}
		
		togglePinPropertiesView(false);
		return resources;
	}
	
	/**
	 * Gets list of application pods for specific resource name. Resource name is 
	 * same as build config or deployment prefix name (name without dash followed by number)
	 * @param resourceName name of resource
	 * @return list of OpenShift application pods for specific resource name
	 */
	public List<OpenShiftResource> getOpenShiftApplicationPods(String resourceName) {
		List<OpenShiftResource> resources = getOpenShiftResources(Resource.POD);
		List<OpenShiftResource> applicationPods = new ArrayList<OpenShiftResource>();
		for (OpenShiftResource resource: resources) {
			String name = resource.getName();
			if (name.contains(resourceName) && !name.contains("-build") &&
					!name.contains("-deploy")) {
				applicationPods.add(resource);
			}
		}
		return applicationPods;
	}
	
	/**
	 * Sets properties view to pinned or not for selected domain. Properties
	 * view has to be opened to perform this method.
	 * 
	 * @param toggle toggle pinned properties view or not
	 */
	public void togglePinPropertiesView(boolean toggle) {
		ToolItem pinItem = new DefaultToolItem("Pins this property view to the current selection");
		pinItem.toggle(toggle);
	}
	
	/**
	 * Deletes OpenShift project.
	 */
	public void delete() {
		item.select();
		new ContextMenu(OpenShiftLabel.ContextMenu.DELETE_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_OS_PROJECT);
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.DELETE_OS_PROJECT), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		new WaitWhile(new OpenShiftProjectExists(projectName));
	}
	
	/**
	 * Open properties for the project.
	 */
	public void openProperties() {
		openProperties(item);
	}
	
	/**
	 * Opens properties for the specified tree item (project, service or pod) in OpenShift explorer.
	 */
	public void openProperties(TreeItem treeItem) {
		activateOpenShiftExplorerView();
		select();
		new ContextMenu(OpenShiftLabel.ContextMenu.PROPERTIES).select();
		
		PropertiesView propertiesView = new PropertiesView();
		propertiesView.activate();
	}
	
	/**
	 * Selects tabbed property representing OpenShift resource under the project in properties view.
	 * 
	 * @param resourceType resource type name
	 */
	public void selectTabbedProperty(String resourceType) {
		new TabbedPropertyList().selectTab(resourceType);
	}
}
