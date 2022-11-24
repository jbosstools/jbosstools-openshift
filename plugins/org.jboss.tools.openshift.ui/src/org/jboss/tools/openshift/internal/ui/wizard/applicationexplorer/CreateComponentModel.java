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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.odo.ComponentMetadata;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.Starter;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentModel extends ComponentModel {
	public static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	public static final String PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE = "eclipseProjectHasDevfile";
	public static final String PROPERTY_ECLIPSE_PROJECT_EMPTY = "eclipseProjectEmpty";
	public static final String PROPERTY_SELECTED_COMPONENT_TYPE = "selectedComponentType";
	public static final String PROPERTY_SELECTED_COMPONENT_STARTERS = "selectedComponentStarters";
	public static final String PROPERTY_SELECTED_COMPONENT_STARTER = "selectedComponentStarter";

	public static final String PROPERTY_DEVMODE_AFTER_CREATE = "devAfterCreate";
	public static final String PROPERTY_IMPORT = "importMode";

	public static final String DEVFILE_NAME = "devfile.yaml";

	private IProject eclipseProject;

	private boolean eclipseProjectHasDevfile = false;

	private boolean eclipseProjectEmpty = false;

	private final List<DevfileComponentType> devfileTypes;

	private ComponentType selectedComponentType;

	private List<Starter> selectedComponentStarters;

	private Starter selectedComponentStarter;

	private boolean devModeAfterCreate = true;

	private boolean importMode;

	/**
	 * @param odo
	 */
	public CreateComponentModel(Odo odo, List<DevfileComponentType> componentTypes, String projectName,
			IProject project) {
		super(odo, projectName, null);
		this.devfileTypes = componentTypes;
		if (project != null) {
			setEclipseProject(project);
			setComponentName(project.getName());
		}
		if (getSelectedComponentType() == null && !componentTypes.isEmpty()) {
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
		setEclipseProjectHasDevfile(project.getFile(DEVFILE_NAME).exists());
		setEclipseProjectEmpty(ProjectUtils.isEmpty(project));
		setSelectedComponentStarter(null);
		if (!isEclipseProjectHasDevfile()) {
			try {
				List<ComponentMetadata> types = getOdo().analyze(project.getLocation().toOSString());
				if (!types.isEmpty()) {
					ComponentMetadata metadata = types.get(0);
					Optional<DevfileComponentType> type = devfileTypes.stream()
							.filter(t -> t.getDevfileRegistry().getName().equals(metadata.getRegistry())
									&& t.getName().equals(metadata.getComponentType()))
							.findFirst();
					if (type.isPresent()) {
						setSelectedComponentType(type.get());
					}
				}
			} catch (IOException e) {
				OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * @return the eclipseProjectHasDevfile
	 */
	public boolean isEclipseProjectHasDevfile() {
		return eclipseProjectHasDevfile;
	}

	/**
	 * @param eclipseProjectHasDevfile the eclipseProjectHasDevfile to set
	 */
	public void setEclipseProjectHasDevfile(boolean eclipseProjectHasDevfile) {
		firePropertyChange(PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE, this.eclipseProjectHasDevfile,
				this.eclipseProjectHasDevfile = eclipseProjectHasDevfile);
	}

	/**
	 * @return the eclipseProjectEmpty
	 */
	public boolean isEclipseProjectEmpty() {
		return eclipseProjectEmpty;
	}

	/**
	 * @param eclipseProjectEmpty the eclipseProjectEmpty to set
	 */
	public void setEclipseProjectEmpty(boolean eclipseProjectEmpty) {
		firePropertyChange(PROPERTY_ECLIPSE_PROJECT_EMPTY, this.eclipseProjectEmpty,
				this.eclipseProjectEmpty = eclipseProjectEmpty);
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
		firePropertyChange(PROPERTY_SELECTED_COMPONENT_TYPE, this.selectedComponentType,
				this.selectedComponentType = selectedComponentType);
		if (selectedComponentType instanceof DevfileComponentType) {
			try {
				setSelectedComponentStarters(getOdo()
						.getComponentTypeInfo(selectedComponentType.getName(),
								((DevfileComponentType) selectedComponentType).getDevfileRegistry().getName())
						.getStarters());
			} catch (IOException e) {
				OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
				setSelectedComponentStarters(Collections.emptyList());
			}
			setSelectedComponentStarter(null);
		}
	}

	/**
	 * @return the selectedComponentStarters
	 */
	public List<Starter> getSelectedComponentStarters() {
		return selectedComponentStarters;
	}

	/**
	 * @param selectedComponentStarters the selectedComponentStarters to set
	 */
	public void setSelectedComponentStarters(List<Starter> selectedComponentStarters) {
		firePropertyChange(PROPERTY_SELECTED_COMPONENT_STARTERS, this.selectedComponentStarters,
				this.selectedComponentStarters = selectedComponentStarters);
	}

	/**
	 * @return the selectedComponentStarter
	 */
	public Starter getSelectedComponentStarter() {
		return selectedComponentStarter;
	}

	/**
	 * @param selectedComponentStarter the selectedComponentStarter to set
	 */
	public void setSelectedComponentStarter(Starter selectedComponentStarter) {
		firePropertyChange(PROPERTY_SELECTED_COMPONENT_STARTER, this.selectedComponentStarter,
				this.selectedComponentStarter = selectedComponentStarter);
	}

	public boolean isDevModeAfterCreate() {
		return devModeAfterCreate;
	}

	public void setDevModeAfterCreate(boolean devModeAfterCreate) {
		firePropertyChange(PROPERTY_DEVMODE_AFTER_CREATE, this.devModeAfterCreate,
				this.devModeAfterCreate = devModeAfterCreate);
	}

	/**
	 * @return the componentTypes
	 */
	public List<DevfileComponentType> getComponentTypes() {
		return devfileTypes;
	}

	public boolean isImportMode() {
		return importMode;
	}

	public void setImportMode(boolean importMode) {
		firePropertyChange(PROPERTY_IMPORT, this.importMode, this.importMode = importMode);
	}

}
