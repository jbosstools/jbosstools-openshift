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

	public static final String PROPERTY_REPOSITORY_PATH = "repositoryPath";
	public static final String PROPERTY_USE_DEFAULT_REPOSITORY_PATH = "useDefaultRepositoryPath";
	public static final String PROPERTY_REUSE_GIT_REPOSITORY = "reuseGitRepository";
	public static final String PROPERTY_REPO_NAME = "repoName";
	public static final String PROPERTY_CLONE_DESTINATION = "cloneDestination";
	
	public void setRepositoryPath(String path);
	public String getRepositoryPath();

	public void setUseDefaultRepositoryPath(boolean useDefault);
	public boolean isUseDefaultRepositoryPath();

	public String getApplicationName();
	
	public String getRepoName();

	public boolean isReuseGitRepository();
	public void setReuseGitRepository(boolean reuseGitRepository);

}
