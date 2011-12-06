/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Rob Stryker
 */
public class AdapterWizardPageModel extends ObservableUIPojo {

	private static final String REMOTE_NAME_DEFAULT = "origin";

	public static final String PROPERTY_NEW_PROJECT = "newProject";
	public static final String PROPERTY_CLONE_URI = "cloneUri";
//	public static final String PROPERTY_MERGE_URI = "mergeUri";
	public static final String PROPERTY_PROJECT_NAME = "projectName";
	public static final String PROPERTY_APPLICATION_URL = "applicationUrl";
	public static final String PROPERTY_REPO_PATH = "repositoryPath";
	public static final String PROPERTY_REMOTE_NAME = "remoteName";
	public static final String PROPERTY_LOADING = "loading";

	public static final String CREATE_SERVER = "createServer";
	public static final String MODE = "serverMode";
	public static final String MODE_SOURCE = "serverModeSource";
	public static final String MODE_BINARY = "serverModeBinary";
	public static final String RUNTIME_DELEGATE = "runtimeDelegate";
	public static final String SERVER_TYPE = "serverType";

	private ImportProjectWizardModel wizardModel;
	private boolean loading;

	public AdapterWizardPageModel(ImportProjectWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setRemoteName(REMOTE_NAME_DEFAULT);
		setNewProject(true);
	}

	public void setNewProject(boolean newProject) {
		firePropertyChange(PROPERTY_NEW_PROJECT, wizardModel.isNewProject(), wizardModel.setNewProject(newProject));
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}
	
	public void setProjectName(String projectName) {
		firePropertyChange(PROPERTY_PROJECT_NAME, wizardModel.getProjectName(), wizardModel.setProjectName(projectName));
	}

	public String getProjectName() {
		return wizardModel.getProjectName();
	}

//	public void setMergeUri(String mergeUri) {
//		firePropertyChange(PROPERTY_MERGE_URI, wizardModel.getMergeUri(), wizardModel.setMergeUri(mergeUri));
//	}
//
//	public String getMergeUri() {
//		return wizardModel.getMergeUri();
//	}

//	public GitUri getKnownMergeUri(String uriOrLabel) {
//		GitUri gitUri = null;
//		if (isGitUri(uriOrLabel)) {
//			gitUri = getKnownMergeUriByUri(uriOrLabel);
//		} else {
//			gitUri = getKnownMergeUriByLabel(uriOrLabel);
//		}
//		return gitUri;
//	}

//	private boolean isGitUri(String gitUriString) {
//		try {
//			URIish uriish = new URIish(gitUriString);
//			return uriish.isRemote();
//		} catch (URISyntaxException e) {
//			return false;
//		}
//	}

//	private GitUri getKnownMergeUriByUri(String gitUriString) {
//		GitUri matchingGitUri = null;
//		for (GitUri gitUri : getMergeUris()) {
//			if (gitUri.getGitUri().equals(gitUriString)) {
//				matchingGitUri = gitUri;
//				break;
//			}
//		}
//		return matchingGitUri;
//	}

//	private GitUri getKnownMergeUriByLabel(String label) {
//		GitUri matchingGitUri = null;
//		for (GitUri gitUri : getMergeUris()) {
//			if (gitUri.getLabel().equals(label)) {
//				matchingGitUri = gitUri;
//				break;
//			}
//		}
//		return matchingGitUri;
//	}

//	public List<GitUri> getMergeUris() {
//		ArrayList<GitUri> mergeUris = new ArrayList<GitUri>();
//		mergeUris.add(new GitUri(
//				"seambooking-example", "git://github.com/openshift/seambooking-example.git",
//				ICartridge.JBOSSAS_7));
//		mergeUris.add(new GitUri(
//				"tweetstream-example", "git://github.com/openshift/tweetstream-example.git",
//				ICartridge.JBOSSAS_7));
//		mergeUris.add(new GitUri(
//				"sinatra-example", "git://github.com/openshift/sinatra-example.git",
//				new Cartridge("rack-1.1")));
//		mergeUris.add(new GitUri(
//				"sugarcrm-example", "git://github.com/openshift/sugarcrm-example.git",
//				new Cartridge("php-5.3")));
//		return mergeUris;
//	}

	public void loadGitUri() throws OpenShiftException {
		setLoading(true);
		setCloneUri("Loading...");
		setCloneUri(getCloneUri());
		setLoading(false);
	}

	private void setCloneUri(String gitUri) {
		firePropertyChange(PROPERTY_CLONE_URI, null, gitUri);
	}

	public String getCloneUri() throws OpenShiftException {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return null;
		}
		return application.getGitUri();
	}

	public void loadApplicationUrl() throws OpenShiftException {
		setLoading(true);
		setApplicationUrl("Loading...");
		setApplicationUrl(getApplicationUrl());
		setLoading(false);
	}

	public String getApplicationUrl() throws OpenShiftException {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return null;
		}
		return application.getApplicationUrl();
	}

	public String getApplicationName() {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return null;
		}
		return application.getName();
	}

	public boolean isJBossAS7Application() {
		IApplication application = wizardModel.getApplication();
		if (application == null) {
			return false;
		}
		return ICartridge.JBOSSAS_7.equals(application.getCartridge());
	}

	public void setApplicationUrl(String applicationUrl) {
		firePropertyChange(PROPERTY_APPLICATION_URL, null, applicationUrl);
	}

	public String getRepositoryPath() {
		return wizardModel.getRepositoryPath();
	}

	public void setRepositoryPath(String repositoryPath) {
		firePropertyChange(PROPERTY_REPO_PATH
				, wizardModel.getRepositoryPath()
				, wizardModel.setRepositoryPath(repositoryPath));
	}

	public void resetRepositoryPath() {
		setRepositoryPath(getDefaultRepositoryPath());
	}

	private String getDefaultRepositoryPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
	}

	public String getRemoteName() {
		return wizardModel.getRemoteName();
	}

	public void setRemoteName(String remoteName) {
		firePropertyChange(PROPERTY_REMOTE_NAME
				, wizardModel.getRemoteName()
				, wizardModel.setRemoteName(remoteName));
	}

	public void resetRemoteName() {
		setRemoteName(REMOTE_NAME_DEFAULT);
	}

	// TODO should this stay?
	public ImportProjectWizardModel getWizardModel() {
		return wizardModel;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		firePropertyChange(PROPERTY_LOADING, this.loading, this.loading = loading);
	}

	public boolean isCompatibleToApplicationCartridge(ICartridge cartridge) {
		IApplication application = wizardModel.getApplication();
		return application != null
				&& application.getCartridge() != null
				&& application.getCartridge().equals(cartridge);
	}

	public static class GitUri {

		private String label;
		private String gitUri;
		private ICartridge cartridge;

		private GitUri(String label, String gitUri, ICartridge cartridge) {
			this.label = label;
			this.gitUri = gitUri;
			this.cartridge = cartridge;
		}

		public String getLabel() {
			return label;
		}

		public String getGitUri() {
			return gitUri;
		}

		public String toString() {
			return getLabel();
		}

		public ICartridge getCartridge() {
			return cartridge;
		}

		public boolean isCompatible(ICartridge cartridge) {
			return this.cartridge.equals(cartridge);
		}
	}
}
