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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Rob Stryker
 */
public class AdapterWizardPageModel extends ObservableUIPojo {

	private static final String REMOTE_NAME_DEFAULT = "origin";

	public static final String PROPERTY_GIT_URI = "gitUri";
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
	}

	public void loadGitUri() throws OpenShiftException {
		setLoading(true);
		setGitUri("Loading...");
		setGitUri(getGitUri());
		setLoading(false);
	}

	private void setGitUri(String gitUri) {
		firePropertyChange(PROPERTY_GIT_URI, null, gitUri);
	}

	public String getGitUri() throws OpenShiftException {
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
		return getEGitDefaultRepositoryPath();
	}

	private String getEGitDefaultRepositoryPath() {
		return Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
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
	public ImportProjectWizardModel getParentModel() {
		return wizardModel;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		firePropertyChange(PROPERTY_LOADING, this.loading, this.loading = loading);
	}

}
