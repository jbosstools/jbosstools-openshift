/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

/**
 * @author Andre Dietisheim
 */
public interface IGitCloningPageModel {

	public String PROPERTY_REPOSITORY_PATH = "repositoryPath";
	public String PROPERTY_USE_DEFAULT_REPOSITORY_PATH = "useDefaultRepositoryPath";
	public String PROPERTY_PROJECT_NAME = "projectName";
	
	public void setRepositoryPath(String path);
	public String getRepositoryPath();

	public void setUseDefaultRepositoryPath(boolean useDefault);
	public boolean isUseDefaultRepositoryPath();

	public void setProjectName(String name);
	public String getProjectName();
}
