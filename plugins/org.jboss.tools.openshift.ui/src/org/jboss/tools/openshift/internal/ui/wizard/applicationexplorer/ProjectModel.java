/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
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
public class ProjectModel extends OdoModel {
	public static final String PROPERTY_PROJECT_NAME = "projectName";

	private String projectName;

	/**
	 * @param odo
	 */
	public ProjectModel(Odo odo, String projectName) {
		super(odo);
		setProjectName(projectName);
	}

	/**
	 * @return the projectName
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @param projectName the projectName to set
	 */
	public void setProjectName(String projectName) {
		firePropertyChange(PROPERTY_PROJECT_NAME, this.projectName, this.projectName = projectName);
	}

}
