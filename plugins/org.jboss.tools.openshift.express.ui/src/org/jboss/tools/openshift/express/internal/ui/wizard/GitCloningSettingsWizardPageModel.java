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

import static org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftApplicationWizardModel.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andr� Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 */
public class GitCloningSettingsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_NEW_PROJECT = "newProject";
	public static final String PROPERTY_CLONE_URI = "cloneUri";
	// public static final String PROPERTY_MERGE_URI = "mergeUri";
	public static final String PROPERTY_PROJECT_NAME = "projectName";
	public static final String PROPERTY_APPLICATION_URL = "applicationUrl";
	public static final String PROPERTY_REPO_PATH = "repositoryPath";
	public static final String PROPERTY_REMOTE_NAME = "remoteName";
	public static final String PROPERTY_LOADING = "loading";

	public static final String CREATE_SERVER = "createServer";
	public static final String MODE = "serverMode";
	public static final String MODE_SOURCE = "serverModeSource";
	public static final String MODE_BINARY = "serverModeBinary";
	public static final String SERVER_TYPE = "serverType";

	public static final String PROPERTY_USE_DEFAULT_REPO_PATH = "useDefaultRepoPath";

	public static final String PROPERTY_CUSTOM_REPO_PATH_VALIDITY = "customRepoPathValidity";

	public static final String PROPERTY_USE_DEFAULT_REMOTE_NAME = "useDefaultRemoteName";

	public static final String PROPERTY_CUSTOM_REMOTE_NAME_VALIDITY = "customRemoteNameValidity";

	private IOpenShiftWizardModel wizardModel;
	private boolean loading;

	private boolean useDefaultRepoPath = true;

	private IStatus customRepoPathValidity = null;

	private IStatus customRemoteNameValidity = null;

	private boolean useDefaultRemoteName = true;

	public GitCloningSettingsWizardPageModel(IOpenShiftWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	public boolean isNewProject() {
		return wizardModel.isNewProject();
	}

	// public void setMergeUri(String mergeUri) {
	// firePropertyChange(PROPERTY_MERGE_URI, wizardModel.getMergeUri(), wizardModel.setMergeUri(mergeUri));
	// }
	//
	// public String getMergeUri() {
	// return wizardModel.getMergeUri();
	// }

	// public GitUri getKnownMergeUri(String uriOrLabel) {
	// GitUri gitUri = null;
	// if (isGitUri(uriOrLabel)) {
	// gitUri = getKnownMergeUriByUri(uriOrLabel);
	// } else {
	// gitUri = getKnownMergeUriByLabel(uriOrLabel);
	// }
	// return gitUri;
	// }

	// private boolean isGitUri(String gitUriString) {
	// try {
	// URIish uriish = new URIish(gitUriString);
	// return uriish.isRemote();
	// } catch (URISyntaxException e) {
	// return false;
	// }
	// }

	// private GitUri getKnownMergeUriByUri(String gitUriString) {
	// GitUri matchingGitUri = null;
	// for (GitUri gitUri : getMergeUris()) {
	// if (gitUri.getGitUri().equals(gitUriString)) {
	// matchingGitUri = gitUri;
	// break;
	// }
	// }
	// return matchingGitUri;
	// }

	// private GitUri getKnownMergeUriByLabel(String label) {
	// GitUri matchingGitUri = null;
	// for (GitUri gitUri : getMergeUris()) {
	// if (gitUri.getLabel().equals(label)) {
	// matchingGitUri = gitUri;
	// break;
	// }
	// }
	// return matchingGitUri;
	// }

	// public List<GitUri> getMergeUris() {
	// ArrayList<GitUri> mergeUris = new ArrayList<GitUri>();
	// mergeUris.add(new GitUri(
	// "seambooking-example", "git://github.com/openshift/seambooking-example.git",
	// ICartridge.JBOSSAS_7));
	// mergeUris.add(new GitUri(
	// "tweetstream-example", "git://github.com/openshift/tweetstream-example.git",
	// ICartridge.JBOSSAS_7));
	// mergeUris.add(new GitUri(
	// "sinatra-example", "git://github.com/openshift/sinatra-example.git",
	// new Cartridge("rack-1.1")));
	// mergeUris.add(new GitUri(
	// "sugarcrm-example", "git://github.com/openshift/sugarcrm-example.git",
	// new Cartridge("php-5.3")));
	// return mergeUris;
	// }

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
		return wizardModel.getApplicationName();
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
		firePropertyChange(PROPERTY_REPO_PATH, wizardModel.getRepositoryPath(),
				wizardModel.setRepositoryPath(repositoryPath));
		validateRepoPathProject();

	}

	public void resetRepositoryPath() {
		if (wizardModel.isNewProject() || getRepositoryPath() == null) {
			setRepositoryPath(getDefaultRepositoryPath());
		}
	}

	public void resetRemoteName() {
		// if existing project and remote name is still 'origin' -> switch to 'openshift'
		// (so, if existing project and remote name is not 'origin', leave as-is
		if (!wizardModel.isNewProject() && NEW_PROJECT_REMOTE_NAME_DEFAULT.equals(getRemoteName())) {
			setRemoteName(EXISTING_PROJECT_REMOTE_NAME_DEFAULT);
		}
		// if new project and remote name is not 'origin' -> restore 'origin'
		else if (wizardModel.isNewProject() && !NEW_PROJECT_REMOTE_NAME_DEFAULT.equals(getRemoteName())) {
			setUseDefaultRemoteName(true);
			setRemoteName(NEW_PROJECT_REMOTE_NAME_DEFAULT);
		}
	}

	@Deprecated
	private String getDefaultRepositoryPath() {
		return EGitUIUtils.getEGitDefaultRepositoryPath();
	}

	public String getRemoteName() {
		return wizardModel.getRemoteName();
	}

	public void setRemoteName(String remoteName) {
		firePropertyChange(PROPERTY_REMOTE_NAME, wizardModel.getRemoteName(), wizardModel.setRemoteName(remoteName));
		validateRemoteName();
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		firePropertyChange(PROPERTY_LOADING, this.loading, this.loading = loading);
	}

	public boolean isCompatibleToApplicationCartridge(ICartridge cartridge) {
		IApplication application = wizardModel.getApplication();
		return application != null && application.getCartridge() != null
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

	public void setUseDefaultRepoPath(boolean useDefaultRepoPath) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REPO_PATH, useDefaultRepoPath,
				this.useDefaultRepoPath = useDefaultRepoPath);
		if (this.useDefaultRepoPath) {
			setRepositoryPath(getDefaultRepositoryPath());
		} else {
			
		}
		validateRepoPathProject();
	}

	public boolean isUseDefaultRepoPath() {
		return useDefaultRepoPath;
	}

	private IStatus validateRepoPathProject() {
		IStatus status = Status.OK_STATUS;
		// skip the validation if the user wants to create a new project. The name and state of the existing project do
		// not matter...
		final IPath repoPath = new Path(getRepositoryPath());
		if (!isUseDefaultRepoPath()) {
			if (repoPath.isEmpty() || !repoPath.isAbsolute() || !repoPath.toFile().canWrite()) {
				status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
						"The path does not exist or is not writeable.");
			}
		}
		final IPath applicationPath = repoPath.append(new Path(getApplicationName()));
		if (applicationPath.toFile().exists()) {
			status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"The location '" + repoPath.toOSString() + "' already contains a folder named '"+ getApplicationName() +"'.");
		}
	
		setCustomRepoPathValidity(status);
		return status;
	}

	public void setCustomRepoPathValidity(IStatus status) {
		firePropertyChange(PROPERTY_CUSTOM_REPO_PATH_VALIDITY, this.customRepoPathValidity,
				this.customRepoPathValidity = status);
	}

	public IStatus getCustomRepoPathValidity() {
		return this.customRepoPathValidity;
	}

	public void setUseDefaultRemoteName(boolean useDefaultRemoteName) {
		firePropertyChange(PROPERTY_USE_DEFAULT_REMOTE_NAME, useDefaultRemoteName,
				this.useDefaultRemoteName = useDefaultRemoteName);
		if (useDefaultRemoteName) {
			setRemoteName(isNewProject() ? NEW_PROJECT_REMOTE_NAME_DEFAULT : EXISTING_PROJECT_REMOTE_NAME_DEFAULT);
		}
		validateRemoteName();
	}

	public boolean isUseDefaultRemoteName() {
		return useDefaultRemoteName;
	}

	private IStatus validateRemoteName() {
		IStatus status = Status.OK_STATUS;
		// skip the validation if the user wants to create a new project. The name and state of the existing project do
		// not matter...
		if (!isUseDefaultRemoteName()) {
			final String remoteName = getRemoteName();
			if(remoteName == null || remoteName.isEmpty()) {
				status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "The custom remote name must not be empty.");
			} else if(!remoteName.matches("\\S+")) {
				status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "The custom remote name must not contain spaces.");
			}
		}
		setCustomRemoteNameValidity(status);
		return status;
	}

	public void setCustomRemoteNameValidity(IStatus status) {
		firePropertyChange(PROPERTY_CUSTOM_REMOTE_NAME_VALIDITY, this.customRemoteNameValidity,
				this.customRemoteNameValidity = status);
	}

	public IStatus getCustomRemoteNameValidity() {
		return this.customRemoteNameValidity;
	}

}
