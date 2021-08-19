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
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.S2iComponentType;
import org.jboss.tools.openshift.core.odo.Starter;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.redhat.devtools.alizer.api.DevfileType;
import com.redhat.devtools.alizer.api.LanguageRecognizer;
import com.redhat.devtools.alizer.api.LanguageRecognizerBuilder;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentModel extends ComponentModel {
	public static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	public static final String PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE = "eclipseProjectHasDevfile";
	public static final String PROPERTY_ECLIPSE_PROJECT_EMPTY = "eclipseProjectEmpty";
	public static final String PROPERTY_SELECTED_COMPONENT_TYPE = "selectedComponentType";
	public static final String PROPERTY_SELECTED_COMPONENT_VERSION = "selectedComponentVersion";
	public static final String PROPERTY_SELECTED_COMPONENT_STARTERS = "selectedComponentStarters";
	public static final String PROPERTY_SELECTED_COMPONENT_STARTER = "selectedComponentStarter";
	public static final String PROPERTY_PUSH_AFTER_CREATE = "pushAfterCreate";
	
	public static final String DEVFILE_NAME = "devfile.yaml";
	
	private static final LanguageRecognizer recognizer = new LanguageRecognizerBuilder().build();
	

	private IProject eclipseProject;
	
	private boolean eclipseProjectHasDevfile = false;
	
	private boolean eclipseProjectEmpty = false;
	
	private final List<ComponentType> componentTypes;
	
	private ComponentType selectedComponentType;
	
	private List<Starter> selectedComponentStarters;
	
	private Starter selectedComponentStarter;
	
	private String selectedComponentVersion;
	
	private boolean pushAfterCreate = true;
	
	private static class DevfileTypeAdapter implements DevfileType {
	  private final DevfileComponentType delegate;

    private DevfileTypeAdapter(DevfileComponentType delegate) {
	    this.delegate = delegate;
	  }

    @Override
    public String getLanguage() {
      return delegate.getLanguage();
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public String getProjectType() {
      return delegate.getProjectType();
    }

    @Override
    public List<String> getTags() {
      return delegate.getTags();
    }

    public ComponentType getDelegate() {
      return delegate;
    }
	}
	
	/**
	 * @param odo
	 */
	public CreateComponentModel(Odo odo, List<ComponentType> componentTypes, String projectName, String applicationName, IProject project) {
		super(odo, projectName, applicationName==null?"app":applicationName, null);
		this.componentTypes = componentTypes;
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
		  List<DevfileTypeAdapter> types = componentTypes.stream().filter(t -> t instanceof DevfileComponentType).map(t -> new DevfileTypeAdapter((DevfileComponentType) t)).collect(Collectors.toList());
		  try {
        DevfileTypeAdapter type = recognizer.selectDevFileFromTypes(project.getLocation().toOSString(), types);
        if (type != null) {
          setSelectedComponentType(type.getDelegate());
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
    firePropertyChange(PROPERTY_ECLIPSE_PROJECT_HAS_DEVFILE, this.eclipseProjectHasDevfile, this.eclipseProjectHasDevfile = eclipseProjectHasDevfile);
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
    firePropertyChange(PROPERTY_ECLIPSE_PROJECT_EMPTY, this.eclipseProjectEmpty, this.eclipseProjectEmpty = eclipseProjectEmpty);
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
		if (selectedComponentType instanceof DevfileComponentType) {
		  try {
        setSelectedComponentStarters(getOdo().getComponentTypeInfo(selectedComponentType.getName(), ((DevfileComponentType) selectedComponentType).getDevfileRegistry().getName()).getStarters());
      } catch (IOException e) {
        setSelectedComponentStarters(Collections.emptyList());
      }
		  setSelectedComponentStarter(null);
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
   * @return the selectedComponentStarters
   */
  public List<Starter> getSelectedComponentStarters() {
    return selectedComponentStarters;
  }

  /**
   * @param selectedComponentStarters the selectedComponentStarters to set
   */
  public void setSelectedComponentStarters(List<Starter> selectedComponentStarters) {
    firePropertyChange(PROPERTY_SELECTED_COMPONENT_STARTERS, this.selectedComponentStarters, this.selectedComponentStarters = selectedComponentStarters);
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
    firePropertyChange(PROPERTY_SELECTED_COMPONENT_STARTER, this.selectedComponentStarter, this.selectedComponentStarter = selectedComponentStarter);
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
