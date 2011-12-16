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
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.wizard.AdapterWizardPageModel;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportProjectWizardModel extends ObservableUIPojo {

	private HashMap<String, Object> dataModel = new HashMap<String, Object>();

	public static final String NEW_PROJECT = "enableProject";
	public static final String USER = "user";
	public static final String APPLICATION = "application";
	public static final String REMOTE_NAME = "remoteName";
	public static final String REPOSITORY_PATH = "repositoryPath";
	public static final String PROJECT_NAME = "projectName";
	public static final String MERGE_URI = "mergeUri";
	public static final String RUNTIME_DELEGATE = "runtimeDelegate";
	public static final String CREATE_SERVER = "createServer";

	public ImportProjectWizardModel() {
		dataModel.put(NEW_PROJECT, false);
	}

	/**
	 * Imports the project that the user has chosen into the workspace.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws OpenShiftException
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 */
	public void importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException,
			URISyntaxException, InvocationTargetException {
		new ImportNewProjectStrategy(getProjectName()
				, getApplication()
				, getRemoteName()
				, getRepositoryFile()
				, isCreateServer()
				, getServerType()
				, getRuntime()
				, getMode()
				, getUser()).execute(monitor);
	}

	/**
	 * Enables the user chosen project to be used on the chosen OpenShift
	 * application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), shares the
	 * user project with git and creates the server adapter.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws URISyntaxException
	 *             The OpenShift application repository could not be cloned,
	 *             because the uri it is located at is not a valid git uri
	 * @throws OpenShiftException
	 * 
	 * @throws InvocationTargetException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation failed.
	 * @throws InterruptedException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation was interrupted.
	 * @throws IOException
	 *             The configuration files could not be copied from the git
	 *             clone to the user project
	 * @throws CoreException
	 *             The user project could not be shared with the git
	 */
	public void addToExistingProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException {
		new AddToExistingProjectStrategy(
				getProjectName()
				, getApplication()
				, getRemoteName()
				, isCreateServer()
				, getServerType()
				, getRuntime()
				, getMode()
				, getUser())
				.execute(monitor);
	}

	public File getRepositoryFile() {
		String repositoryPath = getRepositoryPath();
		if (repositoryPath == null
				|| repositoryPath.length() == 0) {
			return null;
		}
		return new File(repositoryPath, getApplicationName());
	}

	public Object setProperty(String key, Object value) {
		Object oldVal = dataModel.get(key);
		dataModel.put(key, value);
		firePropertyChange(key, oldVal, value);
		return value;
	}

	public Object getProperty(String key) {
		return dataModel.get(key);
	}

	public void setUser(IUser user) {
		setProperty(USER, user);
	}

	public IUser getUser() {
		return (IUser) getProperty(USER);
	}

	public IApplication getApplication() {
		return (IApplication) getProperty(APPLICATION);
	}

	public String getApplicationName() {
		String applicationName = null;
		IApplication application = getApplication();
		if (application != null) {
			applicationName = application.getName();
		}
		return applicationName;
	}

	public ICartridge getApplicationCartridge() {
		ICartridge cartridge = null;
		IApplication application = getApplication();
		if (application != null) {
			cartridge = application.getCartridge();
		}
		return cartridge;
	}

	public String getApplicationCartridgeName() {
		String cartridgeName = null;
		ICartridge cartridge = getApplicationCartridge();
		if (cartridge != null) {
			cartridgeName = cartridge.getName();
		}
		return cartridgeName;
	}

	public void setApplication(IApplication application) {
		setProperty(APPLICATION, application);
	}

	public String setRemoteName(String remoteName) {
		setProperty(REMOTE_NAME, remoteName);
		return remoteName;
	}

	public String getRemoteName() {
		return (String) getProperty(REMOTE_NAME);
	}

	public String setRepositoryPath(String repositoryPath) {
		return (String) setProperty(REPOSITORY_PATH, repositoryPath);
	}

	public String getRepositoryPath() {
		return (String) getProperty(REPOSITORY_PATH);
	}

	public boolean isNewProject() {
		return (Boolean) getProperty(NEW_PROJECT);
	}

	public Boolean setNewProject(boolean newProject) {
		return (Boolean) setProperty(NEW_PROJECT, newProject);
	}

	public String setProjectName(String projectName) {
		return (String) setProperty(PROJECT_NAME, projectName);
	}

	public String getProjectName() {
		return (String) getProperty(PROJECT_NAME);
	}

	public String setMergeUri(String mergeUri) {
		return (String) setProperty(MERGE_URI, mergeUri);
	}

	public String getMergeUri() {
		return (String) getProperty(MERGE_URI);
	}

	public IServerType getServerType() {
		return (IServerType) getProperty(AdapterWizardPageModel.SERVER_TYPE);
	}

	public IRuntime getRuntime() {
		return (IRuntime) getProperty(RUNTIME_DELEGATE);
	}

	public String getMode() {
		return (String) getProperty(AdapterWizardPageModel.MODE);
	}
	
	public boolean isCreateServer() {
		Boolean isCreateServer = (Boolean) getProperty(CREATE_SERVER);
		return isCreateServer != null
				&& isCreateServer.booleanValue();
	}
}
