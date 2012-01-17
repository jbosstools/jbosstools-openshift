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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
abstract class AbstractImportApplicationOperation implements IImportApplicationStrategy {

	private String projectName;
	private IApplication application;
	private String remoteName;

	public AbstractImportApplicationOperation(String projectName, IApplication application, String remoteName) {
		this.projectName = projectName;
		this.application = application;
		this.remoteName = remoteName;
	}

	/**
	 * Clones the repository of the selected OpenShift application to the user
	 * provided path.
	 * 
	 * @param application
	 *            the application to clone
	 * @param remoteName
	 *            the name of the remote repo to clone
	 * @param destination
	 *            the destination to clone to
	 * @param addToRepoView
	 *            if true, the clone repo will get added to the (egit)
	 *            repositories view
	 * @param monitor
	 *            the monitor to report progress to
	 * 
	 * @return the location of the cloned repository
	 * @throws OpenShiftException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * 
	 * @see AbstractImportApplicationOperation#getApplication()
	 * @see #getRepositoryPath()
	 */
	protected File cloneRepository(IApplication application, String remoteName, File destination,
			boolean addToRepoView, IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning repository for application {0}...", application.getName()));
		EGitUIUtils.ensureEgitUIIsStarted();
		if (addToRepoView) {
			EGitUtils.cloneRepository(
					application.getGitUri(), remoteName, destination, EGitUIUtils.ADD_TO_REPOVIEW_TASK, monitor);
		} else {
			EGitUtils.cloneRepository(
					application.getGitUri(), remoteName, destination, monitor);
		}
		return destination;
	}

	protected String getProjectName() {
		return projectName;
	}

	/**
	 * Returns the workspace project with the given name if it exists. Returns
	 * <code>null</code> otherwise.
	 * 
	 * @param name
	 *            the project name
	 * @return the project with the given name
	 */
	private IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.isTrue(project != null && project.exists(),
				NLS.bind("Could not find project {0} in your workspace.", name));
		return project;
	}

	protected IProject getProject() {
		return getProject(getProjectName());
	}

	protected IApplication getApplication() {
		return application;
	}

	protected String getRemoteName() {
		return remoteName;
	}
}
