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
import org.jboss.tools.openshift.core.odo.S2iComponentType;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentModel extends ComponentModel {
	public static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	public static final String PROPERTY_SELECTED_COMPONENT_TYPE = "selectedComponentType";
	public static final String PROPERTY_SELECTED_COMPONENT_VERSION = "selectedComponentVersion";
	public static final String PROPERTY_PUSH_AFTER_CREATE = "pushAfterCreate";
	

	private IProject eclipseProject;
	
	private final List<ComponentType> componentTypes;
	
	private ComponentType selectedComponentType;
	
	private String selectedComponentVersion;
	
	private boolean pushAfterCreate = true;
	
	
	/**
	 * @param odo
	 */
	public CreateComponentModel(Odo odo, List<ComponentType> componentTypes, String projectName, String applicationName) {
		super(odo, projectName, applicationName, null);
		this.componentTypes = componentTypes;
		if (!componentTypes.isEmpty()) {
			setSelectedComponentType(componentTypes.get(0));
		}
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
		if (selectedComponentType instanceof S2iComponentType && !((S2iComponentType)selectedComponentType).getVersions().isEmpty()) {
			setSelectedComponentVersion(((S2iComponentType)selectedComponentType).getVersions().get(0));
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
}
