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
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentModel extends OdoModel {
	public static final String PROPERTY_COMPONENT_NAME = "componentName";
	public static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_SELECTED_COMPONENT_TYPE = "selectedComponentType";
	public static final String PROPERTY_SELECTED_COMPONENT_VERSION = "selectedComponentVersion";
	public static final String PROPERTY_PUSH_AFTER_CREATE = "pushAfterCreate";
	

	private String componentName = "";
	
	private IProject eclipseProject;
	
	private String applicationName = "";
	
	private String projectName;
	
	private final List<ComponentType> componentTypes;
	
	private ComponentType selectedComponentType;
	
	private String selectedComponentVersion;
	
	private boolean pushAfterCreate = true;
	
	
	/**
	 * @param odo
	 */
	public CreateComponentModel(Odo odo, List<ComponentType> componentTypes, String project, String applicationName) {
		super(odo);
		this.componentTypes = componentTypes;
		this.projectName = project;
		this.applicationName = applicationName;
		if (!componentTypes.isEmpty()) {
			setSelectedComponentType(componentTypes.get(0));
		}
	}


	/**
	 * @return the componentName
	 */
	public String getComponentName() {
		return componentName;
	}


	/**
	 * @param componentName the componentName to set
	 */
	public void setComponentName(String componentName) {
		firePropertyChange(PROPERTY_COMPONENT_NAME, this.componentName, this.componentName = componentName);
	}


	/**
	 * @return the Eclipse project
	 */
	public IProject getEclipseProject() {
		return eclipseProject;
	}


	/**
	 * @param project the Eclipse project to set
	 */
	public void setEclipseProject(IProject project) {
		firePropertyChange(PROPERTY_ECLIPSE_PROJECT, this.eclipseProject, this.eclipseProject = project);
	}


	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return applicationName;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		firePropertyChange(PROPERTY_APPLICATION_NAME, this.applicationName, this.applicationName = applicationName);
	}


	/**
	 * @return the selectedComponentType
	 */
	public ComponentType getSelectedComponentType() {
		return selectedComponentType;
	}


	/**
	 * @param selectedComponentType the selectedComponentType to set
	 */
	public void setSelectedComponentType(ComponentType selectedComponentType) {
		firePropertyChange(PROPERTY_SELECTED_COMPONENT_TYPE, this.selectedComponentType, this.selectedComponentType = selectedComponentType);
		if (selectedComponentType.getVersions().length > 0) {
			setSelectedComponentVersion(selectedComponentType.getVersions()[0]);
		}
	}


	/**
	 * @return the selectedComponentVersion
	 */
	public String getSelectedComponentVersion() {
		return selectedComponentVersion;
	}


	/**
	 * @param selectedComponentVersion the selectedComponentVersion to set
	 */
	public void setSelectedComponentVersion(String selectedComponentVersion) {
		firePropertyChange(PROPERTY_SELECTED_COMPONENT_VERSION, this.selectedComponentVersion, this.selectedComponentVersion = selectedComponentVersion);
	}


	/**
	 * @return the pushAfterCreate
	 */
	public boolean isPushAfterCreate() {
		return pushAfterCreate;
	}


	/**
	 * @param pushAfterCreate the pushAfterCreate to set
	 */
	public void setPushAfterCreate(boolean pushAfterCreate) {
		firePropertyChange(PROPERTY_PUSH_AFTER_CREATE, this.pushAfterCreate, this.pushAfterCreate = pushAfterCreate);
	}


	/**
	 * @return the componentTypes
	 */
	public List<ComponentType> getComponentTypes() {
		return componentTypes;
	}


	/**
	 * @return the project name
	 */
	public String getProjectName() {
		return projectName;
	}

}
