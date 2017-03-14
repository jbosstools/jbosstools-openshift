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

import java.io.File;

import org.eclipse.jgit.lib.Repository;

/**
 * @author Andre Dietisheim
 */
public interface IGitCloningPageModel {

	public static final String PROPERTY_REPOSITORY_PATH = "repositoryPath";
	public static final String PROPERTY_USE_DEFAULT_REPOSITORY_PATH = "useDefaultRepositoryPath";
	public static final String PROPERTY_REUSE_GIT_REPOSITORY = "reuseGitRepository";
	public static final String PROPERTY_REPO_NAME = "repoName";
	public static final String PROPERTY_CLONE_DESTINATION = "cloneDestination";
	public static final String PROPERTY_CHECKOUT_BRANCH_REUSED_REPO = "checkoutBranchReusedRepo";
	public static final String PROPERTY_GIT_REF = "gitRef";
	public static final String PROPERTY_IS_CLONE_DESTINATION_AT_GITREF = "cloneDestinationAtGitRef";
	public static final String PROPERTY_GIT_CONTEXT_DIR = "gitContextDir";

	public void setRepositoryPath(String path);
	public String getRepositoryPath();

	public void setUseDefaultRepositoryPath(boolean useDefault);
	public boolean isUseDefaultRepositoryPath();

	public String getApplicationName();

	public String getRepoName();

	/**
	 * Returns the {@link File} that points to the file system location where
	 * the git repository (already) exists or will be cloned to.
	 * 
	 * @return
	 * 
	 * @see File
	 */
	public File getCloneDestination();
	public Repository getCloneDestinationRepository();

	public boolean isReuseGitRepository();
	public void setReuseGitRepository(boolean reuseGitRepository);

	public void setGitContextDir(String contextDir);
	public String getGitContextDir();
	
	public String getGitRef();
	
	public String getGitUrl();
	
	/**
	 * Returns {@code true} if this model is set to checkout the branch
	 * {@link #getGitRef()} in the git repo located at
	 * {@link #getCloneDestination()}
	 * 
	 * @return
	 */
	public boolean isCheckoutBranchReusedRepo();
	public void setCheckoutBranchReusedRepo(boolean checkout);

	/**
	 * Returns {@code true} if the git repository located at clone destination
	 * exists and it's current branch is the git ref available in this model.
	 * 
	 * @return
	 * 
	 * @See #getGitRef()
	 * @see #getCloneDestination()
	 * @see Repository
	 */
	boolean isCloneDestinationAtGitRef();

}
