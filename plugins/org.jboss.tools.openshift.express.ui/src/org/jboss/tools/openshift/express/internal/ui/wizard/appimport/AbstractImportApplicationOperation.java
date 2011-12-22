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
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
abstract class AbstractImportApplicationOperation implements IImportApplicationStrategy {

	private static final int CLONE_TIMEOUT = 10 * 1024;

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
	 * provided path
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @return
	 * @throws URISyntaxException
	 * @throws OpenShiftException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * 
	 * @see AbstractImportApplicationOperation#getApplication()
	 * @see #getRepositoryPath()
	 */
	protected File cloneRepository(IApplication application, String remoteName, File destination, IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, URISyntaxException {
		monitor.subTask(NLS.bind("Cloning repository for application {0}...", application.getName()));
		cloneRepository(application.getGitUri(), remoteName, destination, monitor);
		return destination;
	}

	private void cloneRepository(String uri, String remoteName, File destination, IProgressMonitor monitor)
			throws OpenShiftException, URISyntaxException, InvocationTargetException, InterruptedException {
		ensureEgitUIIsStarted();
		URIish gitUri = new URIish(uri);
		CloneOperation cloneOperation =
				new CloneOperation(gitUri, true, null, destination, Constants.HEAD, remoteName, CLONE_TIMEOUT);
		cloneOperation.run(monitor);
		RepositoryUtil repositoryUtil = Activator.getDefault().getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(new File(destination, Constants.DOT_GIT));
	}

	/**
	 * The EGit UI plugin initializes the ssh factory to present the user a
	 * passphrase prompt if the ssh key was not read yet. If this initialization
	 * is not executed, the ssh connection to the git repo would just fail with
	 * an authentication error. We therefore have to make sure that the EGit UI
	 * plugin is started and initializes the JSchConfigSessionFactory.
	 * <p>
	 * EGit initializes the SshSessionFactory with the EclipseSshSessionFactory.
	 * The EclipseSshSessionFactory overrides JschConfigSessionFactory#configure
	 * to present a UserInfoPrompter if the key passphrase was not entered
	 * before.
	 * 
	 * @see Activator#start(org.osgi.framework.BundleContext)
	 * @see Activator#setupSSH
	 * @see JschConfigSessionFactory#configure
	 * @see EclipseSshSessionFactory#configure
	 */
	private void ensureEgitUIIsStarted() {
		Activator.getDefault();
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
	protected IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		Assert.isTrue(project != null && project.exists(),
				NLS.bind("Could not find project {0} in your workspace.", name));
		return project;
	}

	protected IApplication getApplication() {
		return application;
	}

	protected String getRemoteName() {
		return remoteName;
	}
}
