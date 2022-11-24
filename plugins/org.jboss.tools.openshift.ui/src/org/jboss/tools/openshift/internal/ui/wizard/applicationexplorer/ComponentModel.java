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

import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class ComponentModel extends ProjectModel {
	public static final String PROPERTY_COMPONENT_NAME = "componentName";

	private String componentName;

	/**
	 * @param odo
	 */
	public ComponentModel(Odo odo, String projectName, String componentName) {
		super(odo, projectName);
		setComponentName(componentName);
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

}
