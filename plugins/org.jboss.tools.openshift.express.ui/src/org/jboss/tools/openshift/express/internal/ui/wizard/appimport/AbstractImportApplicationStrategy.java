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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.internal.util.ComponentUtilities;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
abstract class AbstractImportApplicationStrategy implements IImportApplicationStrategy {

	private static final int CLONE_TIMEOUT = 10 * 1024;

	private String projectName;
	private IApplication application;
	private String remoteName;
	private boolean isCreateServer;
	private IServerType serverType;
	private IRuntime runtime;
	private String mode;
	private IUser user;

	public AbstractImportApplicationStrategy(String projectName, IApplication application, String remoteName,
			boolean isCreateServer, IServerType serverType, IRuntime runtime, String mode,
			IUser user) {
		this.projectName = projectName;
		this.application = application;
		this.remoteName = remoteName;
		this.isCreateServer = isCreateServer;
		this.serverType = serverType;
		this.runtime = runtime;
		this.mode = mode;
		this.user = user;
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
	 * @see AbstractImportApplicationStrategy#getApplication()
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

	/**
	 * creates an OpenShift server adapter for the user chosen project.
	 * 
	 * @param monitor
	 *            the monitor to report progress to.
	 * @throws OpenShiftException
	 */
	protected void createServerAdapter(IProject project, IServerType serverType, IRuntime runtime, String mode,
			IApplication application, IUser user, IProgressMonitor monitor) throws OpenShiftException {
		String name = project.getName();
		monitor.subTask(NLS.bind("Creating server adapter for project {0}", name));
		createServerAdapter(Collections.singletonList(project), serverType, runtime, mode, application, user,
				monitor);
	}

	protected void createServerAdapter(List<IProject> importedProjects, IServerType serverType,
			IRuntime runtime, String mode, IApplication application, IUser user, IProgressMonitor monitor) {
		try {
			renameWebContextRoot(importedProjects);
			IServer server = doCreateServerAdapter(serverType, runtime, mode, application, user);
			addModules(getModules(importedProjects), server, monitor);
		} catch (CoreException ce) {
			OpenShiftUIActivator.getDefault().getLog().log(ce.getStatus());
		} catch (OpenShiftException ose) {
			IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Cannot create openshift server adapter", ose);
			OpenShiftUIActivator.getDefault().getLog().log(s);
		}
	}

	private void renameWebContextRoot(List<IProject> importedProjects) {
		for (IProject project : importedProjects) {
			ComponentUtilities.setServerContextRoot(project, "/");
		}
	}

	private IServer doCreateServerAdapter(IServerType serverType, IRuntime rt, String mode, IApplication application,
			IUser user) throws CoreException,
			OpenShiftException {
		Assert.isLegal(serverType != null);
		Assert.isLegal(mode != null);
		Assert.isLegal(application != null);
		Assert.isLegal(user != null);

		String serverNameBase = application.getName() + " OpenShift Server";
		String serverName = org.jboss.ide.eclipse.as.core.util.ServerUtil.getDefaultServerName(serverNameBase);

		IServer server = ExpressServerUtils.createServer(rt, serverType, serverName);
		ExpressServerUtils.fillServerWithOpenShiftDetails(server, application.getApplicationUrl(),
				user.getRhlogin(), user.getPassword(),
				user.getDomain().getNamespace(), application.getName(), application.getUUID(), mode);
		return server;
	}

	private void addModules(List<IModule> modules, IServer server, IProgressMonitor monitor) throws CoreException {
		if (modules == null
				|| modules.size() == 0) {
			return;
		}
		IServerWorkingCopy wc = server.createWorkingCopy();
		IModule[] add = modules.toArray(new IModule[modules.size()]);
		wc.modifyModules(add, new IModule[0], new NullProgressMonitor());
		server = wc.save(true, monitor);
		((Server) server).setModulePublishState(add, IServer.PUBLISH_STATE_NONE);
	}

	private List<IModule> getModules(List<IProject> importedProjects) {
		Iterator<IProject> i = importedProjects.iterator();
		ArrayList<IModule> toAdd = new ArrayList<IModule>();
		while (i.hasNext()) {
			IProject p = i.next();
			IModule[] m = ServerUtil.getModules(p);
			if (m != null && m.length > 0) {
				toAdd.addAll(Arrays.asList(m));
			}
		}
		return toAdd;
	}

	public String getProjectName() {
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

	public IApplication getApplication() {
		return application;
	}

	public String getRemoteName() {
		return remoteName;
	}

	public boolean isCreateServer() {
		return isCreateServer;
	}

	public IServerType getServerType() {
		return serverType;
	}

	public IRuntime getRuntime() {
		return runtime;
	}

	public String getMode() {
		return mode;
	}

	public IUser getUser() {
		return user;
	}

}
