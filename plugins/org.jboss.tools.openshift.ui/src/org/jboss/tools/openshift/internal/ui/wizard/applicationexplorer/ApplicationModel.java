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
public class ApplicationModel extends ProjectModel {
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	
	private String applicationName;
	
	/**
	 * @param odo
	 */
	public ApplicationModel(Odo odo, String projectName, String applicationName) {
		super(odo, projectName);
		setApplicationName(applicationName);
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
		firePropertyChange(PROPERTY_APPLICATION_NAME, this.applicationName,this.applicationName = applicationName);
	}
	
	


}
