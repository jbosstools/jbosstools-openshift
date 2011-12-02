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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
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
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.wizard.projectimport.GeneralProjectImportOperation;
import org.jboss.tools.openshift.express.internal.ui.wizard.projectimport.MavenProjectImportOperation;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class ImportProjectWizardModel extends ObservableUIPojo {

	private static final int CLONE_TIMEOUT = 10 * 1024;

	private HashMap<String, Object> dataModel = new HashMap<String, Object>();

	public static final String USER = "user";
	public static final String APPLICATION = "application";
	public static final String REMOTE_NAME = "remoteName";
	public static final String REPOSITORY_PATH = "repositoryPath";

	public void setProperty(String key, Object value) {
		Object oldVal = dataModel.get(key);
		dataModel.put(key, value);
		firePropertyChange(key, oldVal, value);
	}

	public Object getProperty(String key) {
		return dataModel.get(key);
	}

	public void setUser(IUser user) {
		setProperty(USER, user);
	}

	public IUser getUser() {
		return (IUser)getProperty(USER);
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
		setProperty(REPOSITORY_PATH, repositoryPath);
		return repositoryPath;
	}

	public String getRepositoryPath() {
		return (String) getProperty(REPOSITORY_PATH);
	}

	public void importProject(final File projectFolder, IProgressMonitor monitor) throws OpenShiftException,
			CoreException,
			InterruptedException {
		new WorkspaceJob(NLS.bind("Importing projects from {0}", projectFolder.getAbsolutePath())) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					List<IProject> importedProjects = importMavenProject(projectFolder, monitor);
					connectToGitRepo(importedProjects, projectFolder, monitor);
					createServerAdapterIfRequired(importedProjects, monitor);
					return Status.OK_STATUS;
				} catch (Exception e) {
					IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							NLS.bind("Could not import projects from {0}", projectFolder.getAbsolutePath()), e);
					OpenShiftUIActivator.log(status);
					return status;
				}
			}

		}.schedule();
	}

	private List<IProject> importMavenProject(final File projectFolder, IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		MavenProjectImportOperation mavenImport = new MavenProjectImportOperation(projectFolder);
		List<IProject> importedProjects = Collections.emptyList();
		if (mavenImport.isMavenProject()) {
			importedProjects = mavenImport.importToWorkspace(monitor);
		} else {
			importedProjects = new GeneralProjectImportOperation(projectFolder).importToWorkspace(monitor);
		}
		return importedProjects;
	}
	
	private void connectToGitRepo(List<IProject> projects, File projectFolder, IProgressMonitor monitor)
			throws CoreException {
		File gitFolder = new File(projectFolder, Constants.DOT_GIT);
		for (IProject project : projects) {
			if (project != null) {
				connectToGitRepo(project, gitFolder, monitor);
			}
		}
	}

	private void connectToGitRepo(IProject project, File gitFolder, IProgressMonitor monitor) throws CoreException {
		new ConnectProviderOperation(project, gitFolder).execute(monitor);
	}

	public File cloneRepository(IProgressMonitor monitor) throws URISyntaxException, OpenShiftException,
			InvocationTargetException,
			InterruptedException {
		IApplication application = getApplication();
		File destination = new File(getRepositoryPath(), application.getName());
		cloneRepository(application.getGitUri(), destination, monitor);
		return destination;
	}

	private void cloneRepository(String uri, File destination, IProgressMonitor monitor) throws URISyntaxException,
			OpenShiftException,
			InvocationTargetException,
			InterruptedException {
		if (destination.exists()) {
			FileUtil.completeDelete(destination);
		}
		ensureEgitUIIsStarted();
		URIish gitUri = new URIish(uri);
		CloneOperation cloneOperation =
				new CloneOperation(gitUri, true, null, destination, Constants.HEAD, getRemoteName(), CLONE_TIMEOUT);
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

	private void createServerAdapterIfRequired(List<IProject> importedProjects, IProgressMonitor monitor) {
		Boolean b = (Boolean)getProperty(AdapterWizardPageModel.CREATE_SERVER);
		if( b != null && b.booleanValue() ) {
			try {
				renameWebContextRoot(importedProjects);
				IServer server = createServerAdapter();
				addModules(getModules(importedProjects), server, monitor);
			} catch(CoreException ce) {
				OpenShiftUIActivator.getDefault().getLog().log(ce.getStatus());
			} catch(OpenShiftException ose) {
				IStatus s = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Cannot create openshift server adapter", ose);
				OpenShiftUIActivator.getDefault().getLog().log(s);
			}
		}
	}

	private void renameWebContextRoot(List<IProject> importedProjects) {
		for (IProject project : importedProjects) {
			renameWebContextRoot(project);
		}
	}

	private void renameWebContextRoot(IProject project) {
		ComponentUtilities.setServerContextRoot(project, "/");
	}

	private IServer createServerAdapter() throws CoreException,
			OpenShiftException {
		IServerType type = (IServerType)getProperty(AdapterWizardPageModel.SERVER_TYPE);
		IRuntime rt = (IRuntime)getProperty(AdapterWizardPageModel.RUNTIME_DELEGATE);
		String mode = (String)getProperty(AdapterWizardPageModel.MODE);

		String serverNameBase = getApplication().getName() + " OpenShift Server";
		String serverName = org.jboss.ide.eclipse.as.core.util.ServerUtil.getDefaultServerName(serverNameBase);
		
		IServer server = ExpressServerUtils.createServer(rt, type, serverName);
		ExpressServerUtils.fillServerWithOpenShiftDetails(server, getApplication().getApplicationUrl(), 
				getUser().getRhlogin(), getUser().getPassword(), 
				getUser().getDomain().getNamespace(), getApplication().getName(), getApplication().getUUID(), mode);
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
	}

	private List<IModule> getModules(List<IProject> importedProjects) {
		Iterator<IProject> i = importedProjects.iterator();
		ArrayList<IModule> toAdd = new ArrayList<IModule>();
		while(i.hasNext()) {
			IProject p = i.next();
			IModule[] m = ServerUtil.getModules(p);
			if( m != null && m.length > 0 ) {
				toAdd.addAll(Arrays.asList(m));
			}
		}
		return toAdd;
	}
}
