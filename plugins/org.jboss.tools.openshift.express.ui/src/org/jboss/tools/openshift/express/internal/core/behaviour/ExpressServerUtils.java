/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethodType;
import org.jboss.ide.eclipse.as.core.util.DeploymentPreferenceLoader;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.ide.eclipse.as.core.util.RuntimeUtils;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.core.internal.EGitCoreActivator;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * This class holds the attribute names whose values will be stored inside a
 * server object, as well as the utility methods used to get and set them for a
 * server.
 * 
 * @author Rob Stryker
 */
@SuppressWarnings("restriction")
public class ExpressServerUtils {
	/* Server Settings */
	public static final String ATTRIBUTE_DEPLOY_PROJECT = "org.jboss.tools.openshift.binary.deployProject";
	public static final String ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS = "org.jboss.tools.openshift.project.override";

	/* Legacy Server Settings: Please usage scan before removal */
	public static final String ATTRIBUTE_DEPLOY_PROJECT_LEGACY = "org.jboss.tools.openshift.express.internal.core.behaviour.binary.deployProject";
	public static final String ATTRIBUTE_REMOTE_NAME = "org.jboss.tools.openshift.express.internal.core.behaviour.RemoteName";
	public static final String ATTRIBUTE_APPLICATION_NAME = "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationName";
	public static final String ATTRIBUTE_APPLICATION_ID = "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationId";
	public static final String ATTRIBUTE_DOMAIN = "org.jboss.tools.openshift.express.internal.core.behaviour.Domain";
	public static final String ATTRIBUTE_USERNAME = "org.jboss.tools.openshift.express.internal.core.behaviour.Username";
	public static final String ATTRIBUTE_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.express.internal.core.behaviour.DEPLOY_FOLDER_LOC";

	/* New Settings inside the project */
	public static final String SETTING_REMOTE_NAME = "org.jboss.tools.openshift.RemoteName";
	public static final String SETTING_APPLICATION_NAME = "org.jboss.tools.openshift.ApplicationName";
	public static final String SETTING_APPLICATION_ID = "org.jboss.tools.openshift.ApplicationId";
	public static final String SETTING_DOMAIN = "org.jboss.tools.openshift.Domain";
	public static final String SETTING_USERNAME = "org.jboss.tools.openshift.Username";
	public static final String SETTING_CONNECTIONURL = "org.jboss.tools.openshift.Connection";
	public static final String SETTING_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.DeployFolder";

	// Legacy, not to be used
	// public static final String ATTRIBUTE_PASSWORD =
	// "org.jboss.tools.openshift.express.internal.core.behaviour.Password";
	public static final String ATTRIBUTE_REMOTE_NAME_DEFAULT = "origin";
	private static final String ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT = "deployments";
	private static final String ATTRIBUTE_DEPLOY_FOLDER_TOMCAT_DEFAULT = "webapps";

	public static final String PREFERENCE_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.express.internal.core.behaviour.IgnoreContextRoot";

	/** the OpensHift Server Type as defined in the plugin.xml. */
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.express.openshift.server.type";

	/* For use inside express wizard fragment */
	public static final String TASK_WIZARD_ATTR_CONNECTION = "connection";
	public static final String TASK_WIZARD_ATTR_DOMAIN = "domain";
	public static final String TASK_WIZARD_ATTR_APP_LIST = "appList";
	public static final String TASK_WIZARD_ATTR_SELECTED_APP = "application";

	/*
	 * For use in finding not just the effective value, but values stored either
	 * in project or server
	 */
	public static int SETTING_FROM_PROJECT = 1;
	public static int SETTING_FROM_SERVER = 2;
	public static int SETTING_EFFECTIVE_VALUE = 3;

	public static String getProjectAttribute(IProject project, String attributeName, String defaultVal) {
		if (project == null)
			return defaultVal;
		String qualifier = OpenShiftUIActivator.getDefault().getBundle().getSymbolicName();
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(qualifier);
		return node.get(attributeName, defaultVal);
	}

	/**
	 * Look-up the OpenShift application associated with the given server. This
	 * operation can be time-consuming since it may need to perform a request on
	 * OpenShift if the user's applications list had not been loaded before.
	 * Callers should use this methd without blocking the UI.
	 * 
	 * @param server
	 *            the server
	 * @return the openshift application or null if it could not be located.
	 * @throws OpenShiftException
	 */
	public static IApplication getApplication(IServerAttributes server) {
		final String appName = getExpressApplicationName(server);
		if (StringUtils.isEmpty(appName)) {
			return null;
		}
		final ConnectionURL connectionUrl = getExpressConnectionUrl(server);
		if (connectionUrl == null) {
			return null;
		}
		try {
			Connection connection = ConnectionsModelSingleton.getInstance().getConnectionByUrl(connectionUrl);
			if (connection != null) {
				return connection.getApplicationByName(appName); // May be long running
			} else {
				Logger.error(NLS.bind("Could not find connection {0}", connectionUrl.toString()));
			}
		} catch (OpenShiftException e) {
			Logger.error(NLS.bind("Failed to retrieve application ''{0}'' at url ''{1}}'", appName, connectionUrl), e);
		}
		return null;
	}

	/* Settings stored only in the project */
	public static String getExpressApplicationName(IServerAttributes attributes) {
		return getProjectAttribute(
				getExpressDeployProject2(attributes), 
				SETTING_APPLICATION_NAME,	
				attributes.getAttribute(ATTRIBUTE_APPLICATION_NAME, (String) null));
	}

	public static String getExpressApplicationId(IServerAttributes attributes) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_APPLICATION_ID,
				attributes.getAttribute(ATTRIBUTE_APPLICATION_ID, (String) null));
	}

	public static String getExpressDomain(IServerAttributes attributes) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_DOMAIN,
				attributes.getAttribute(ATTRIBUTE_DOMAIN, (String) null));
	}

	private static String getExpressUsername(IServerAttributes attributes) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_USERNAME,
				attributes.getAttribute(ATTRIBUTE_USERNAME, (String) null));
	}

	public static ConnectionURL getExpressConnectionUrl(IServerAttributes attributes) {
		try {
			String connectionUrlString = getProjectAttribute(
					getExpressDeployProject2(attributes), SETTING_CONNECTIONURL, null);
			if (!StringUtils.isEmpty(connectionUrlString)) {
				return ConnectionURL.forURL(connectionUrlString);
			}
			
			String username = getExpressUsername(attributes);
			if (!StringUtils.isEmpty(username)) {
				return ConnectionURL.forUsername(username);
			}
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
		} catch (MalformedURLException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
		}

		return null;
	}
		
	public static String getExpressDeployFolder(IServerAttributes attributes) {
		return getExpressDeployFolder(attributes, getApplication(attributes));
	}

	public static String getExpressDeployFolder(IServerAttributes attributes, IApplication application) {
		return getExpressDeployFolder(attributes, getDefaultDeployFolder(application));
	}
		
	/* Settings stored in the project, maybe over-ridden in the server */
	private static String getExpressDeployFolder(IServerAttributes attributes, String defaultDeployFolder) {
		if (getOverridesProject(attributes)) {
			return attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, defaultDeployFolder);
		}
		
		return getProjectAttribute(
				getExpressDeployProject2(attributes), SETTING_DEPLOY_FOLDER_NAME, defaultDeployFolder);
	}

	public static String getDefaultDeployFolder(IServerAttributes server) {
		return getDefaultDeployFolder(getApplication(server));
	}
	
	private enum DeployFolder {
		JBOSSAS(IStandaloneCartridge.NAME_JBOSSAS, "deployments"), 
		JBOSSEAP(IStandaloneCartridge.NAME_JBOSSEAP, "deployments"), 
		JBOSSEWS(IStandaloneCartridge.NAME_JBOSSEWS, "webapps");

		private String cartridgeName;
		private String deployFolder;

		DeployFolder(String cartridgeName, String deployFolder) {
			this.cartridgeName = cartridgeName;
			this.deployFolder = deployFolder;
		}
		
		public String getDeployFolder() {
			return deployFolder;
		}
		
		public static DeployFolder getByCartridgeName(String cartridgeName) {
			if (cartridgeName == null) {
				return null;
			}
			
			for (DeployFolder deployFolder : values()) {
				if (cartridgeName.startsWith(deployFolder.cartridgeName)) {
					return deployFolder;
				}
			}
			return null;
		}
		
	}
	
	public static String getDefaultDeployFolder(IApplication application) {
		DeployFolder deployFolder = DeployFolder.getByCartridgeName(application.getCartridge().getName());
		if (deployFolder == null) {
			return null;
		}
		return deployFolder.getDeployFolder();
	}
		
	public static IContainer getDeployFolderResource(String deployFolder, IProject project) {
		if (!StringUtils.isEmpty(deployFolder)) {
			return (IContainer) project.findMember(new Path(deployFolder));
		} else {
			return project;
		}

	}
	
	public static String getExpressRemoteName(IServerAttributes attributes) {
		if (getOverridesProject(attributes))
			return attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, ATTRIBUTE_REMOTE_NAME_DEFAULT);
		return getProjectAttribute(getExpressDeployProject2(attributes), 
				SETTING_REMOTE_NAME, ATTRIBUTE_REMOTE_NAME_DEFAULT);
	}

	public static String getExpressDeployFolder(IServerAttributes attributes, int fromWhere) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, (String) null);
		if (fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getExpressDeployProject2(attributes), SETTING_DEPLOY_FOLDER_NAME, null);
		if (fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if (getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT : fromProject;
	}

	public static String getExpressRemoteName(IServerAttributes attributes, int fromWhere) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, (String) null);
		if (fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getExpressDeployProject2(attributes), SETTING_REMOTE_NAME, null);
		if (fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if (getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromProject;
	}

	/* Settings stored only in the server */
	public static String getExpressDeployProject(IServerAttributes attributes) {
		return attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT,
				attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT_LEGACY, (String) null));
	}

	private static IProject getExpressDeployProject2(IServerAttributes attributes) {
		String name = getExpressDeployProject(attributes);
		return name == null ? null : ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	public static boolean getIgnoresContextRoot(IServerAttributes server) {
		return server.getAttribute(PREFERENCE_IGNORE_CONTEXT_ROOT, true);
	}

	public static boolean getOverridesProject(IServerAttributes server) {
		return server.getAttribute(ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS, false);
	}

	public static IServer setIgnoresContextRoot(IServerAttributes server, boolean val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	/**
	 * Fills an already-created server with the proper openshift details.
	 * 
	 * @param server
	 * @param host
	 * @param username
	 * @param password
	 * @param domain
	 * @param appName
	 * @param sourceOrBinary
	 * @return
	 * @throws CoreException
	 */
	public static IServer fillServerWithOpenShiftDetails(IServer server, String host,
			String deployProject, String remote, String appName) throws CoreException {
		ServerWorkingCopy wc = (ServerWorkingCopy) server.createWorkingCopy();
		fillServerWithOpenShiftDetails((IServerWorkingCopy) wc, host, deployProject, remote, appName);
		IServer saved = wc.save(true, new NullProgressMonitor());
		return saved;
	}

	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, String host,
			String deployProject, String remote, String appName) {
		host = removeProtocol(host);
		wc.setHost(host);
		wc.setAttribute(IDeployableServer.SERVER_MODE, ExpressBehaviourDelegate.OPENSHIFT_ID);
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, deployProject);
		// wc.setAttribute(ATTRIBUTE_USERNAME, username);
		// wc.setAttribute(ATTRIBUTE_DOMAIN, domain);
		// wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, appName);
		// wc.setAttribute(ATTRIBUTE_APPLICATION_ID, appId);
		// wc.setAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, projectRelativeFolder);
		// wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, mode);
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, remote);
		((ServerWorkingCopy) wc).setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		wc.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, String.valueOf(Boolean.TRUE));
		wc.setAttribute(IJBossToolingConstants.WEB_PORT, 80);
		wc.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, "false");
		wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
		wc.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
		setName(wc, appName);
	}

	protected static void setName(IServerWorkingCopy wc, String appName) {
		if (appName != null
				&& (StringUtils.isEmpty(wc.getName()) 
						|| wc.getName().startsWith(ExpressServer.DEFAULT_SERVER_NAME_BASE))) {
			String newBase = appName + " at OpenShift";
			wc.setName(ServerUtil.getDefaultServerName(newBase));
		}
	}

	protected static String removeProtocol(String host) {
		if (host != null) {
			if (host.indexOf("://") != -1)
				host = host.substring(host.indexOf("://") + 3);
			if (host.endsWith("/"))
				host = host.substring(0, host.length() - 1);
		}
		return host;
	}

	public static IServer createServerAndRuntime(String runtimeID, String serverID,
			String location, String configuration) throws CoreException {
		IRuntime runtime = RuntimeUtils.createRuntime(runtimeID, location, configuration);
		return createServer(runtime, serverID);
	}

	public static IServer createServer(IRuntime runtime, String serverID) throws CoreException {
		return createServer2(runtime, ServerCore.findServerType(serverID), serverID);
	}

	public static IServer createServer(IRuntime runtime, IServerType serverType, String serverName)
			throws CoreException {
		return createServer2(runtime, serverType, serverName);
	}

	public static IServer createServer2(IRuntime currentRuntime, IServerType serverType, String serverName)
			throws CoreException {
		IServerWorkingCopy serverWC = serverType.createServer(null, null,
				new NullProgressMonitor());
		serverWC.setRuntime(currentRuntime);
		serverWC.setName(serverName);
		serverWC.setServerConfiguration(null);
		serverWC.setAttribute(IDeployableServer.SERVER_MODE, ExpressBehaviourDelegate.OPENSHIFT_ID);
		return serverWC.save(true, new NullProgressMonitor());
	}

	/**
	 * Returns true if the given server is an OpenShift one, false otherwise.
	 * 
	 * @param server
	 *            the server to check
	 * @return true or false
	 */
	public static boolean isOpenShiftRuntime(IServerAttributes server) {
		final String serverTypeId = server.getServerType().getId();
		return (OPENSHIFT_SERVER_TYPE.equals(serverTypeId));
	}

	/**
	 * Returns true if the given server is a server using an openshift behaviour
	 * 
	 * @param server
	 *            the server to check
	 * @return true or false
	 */
	public static boolean isInOpenshiftBehaviourMode(IServer server) {
		IDeployableServer ds = ServerConverter.getDeployableServer(server);
		if (ds != null) {
			IJBossServerPublishMethodType type = DeploymentPreferenceLoader.getCurrentDeploymentMethodType(server);
			if (type != null) {
				String id = type.getId();
				if (ExpressBinaryBehaviourDelegate.OPENSHIFT_BINARY_ID.equals(id)
						|| ExpressBehaviourDelegate.OPENSHIFT_ID.equals(id))
					return true;
			}
		}
		return false;
	}

	public static IApplication findApplicationForProject(IProject p, List<IApplication> applications)
			throws OpenShiftException, CoreException {
		List<URIish> uris = EGitUtils.getDefaultRemoteURIs(p);
		Iterator<IApplication> i = applications.iterator();
		while (i.hasNext()) {
			IApplication a = i.next();
			String gitUri = a.getGitUrl();
			Iterator<URIish> j = uris.iterator();
			while (j.hasNext()) {
				String projUri = j.next().toPrivateString();
				if (projUri.equals(gitUri)) {
					return a;
				}
			}
		}
		return null;
	}

	/**
	 * This method will search available projects for one that has a git remote
	 * URI that matches that of this IApplication
	 * 
	 * @param application
	 * @return
	 */
	public static IProject[] findProjectsForApplication(final IApplication application) {
		if (application == null)
			return null;
		final ArrayList<IProject> results = new ArrayList<IProject>();
		final String gitUri = application.getGitUrl();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (hasGitUri(gitUri, projects[i])) {
				results.add(projects[i]);
			}
		}
		return results.toArray(new IProject[results.size()]);
	}
	
	private static boolean hasGitUri(String gitURI, IProject project) {
		try {
			Pattern gitURIPattern = Pattern.compile(RegExUtils.escapeRegex(gitURI));
			Repository repository = EGitUtils.getRepository(project);
			String remoteName = getProjectAttribute(project, SETTING_REMOTE_NAME, null);
			if (!StringUtils.isEmptyOrNull(remoteName)) {
				return EGitUtils.hasRemoteUrl(gitURIPattern, EGitUtils.getRemoteByName(remoteName, repository));
			} else {
				return EGitUtils.hasRemoteUrl(gitURIPattern, repository);
			}
		} catch (CoreException ce) {
			OpenShiftUIActivator.log(NLS.bind("Could not look up remotes for project {0}", project), ce);
		}
		return false;
	}


	/**
	 * This method will search for all projects connected to git and having the
	 * proper settings file containing domain, application id, app name, and
	 * connection url
	 * 
	 * @return
	 */
	public static IProject[] findAllSuitableOpenshiftProjects() {
		final ArrayList<IProject> results = new ArrayList<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (EGitUtils.getRepository(projects[i]) != null
					&& hasOpenShiftSettings(projects[i])) {
				results.add(projects[i]);
			}
		}
		return results.toArray(new IProject[results.size()]);
	}

	private static boolean hasOpenShiftSettings(IProject project) {
		String appName = getProjectAttribute(project, SETTING_APPLICATION_NAME, null);
		String appId = getProjectAttribute(project, SETTING_APPLICATION_ID, null);
		String domain = getProjectAttribute(project, SETTING_DOMAIN, null);
		String connectionUrl = getProjectAttribute(project, SETTING_CONNECTIONURL, null);
		String username = getProjectAttribute(project, SETTING_USERNAME, null);
		return appName != null
				&& appId != null
				&& domain != null
				&& (connectionUrl != null || username != null);
	}

	public static IProject findProjectForApplication(IApplication application) {
		IProject[] p = findProjectsForApplication(application);
		return p == null ? null : p.length == 0 ? null : p[0];
	}

	public static IProject findProjectForServersApplication(IServerAttributes server) {
		IApplication app = findApplicationForServer(server);
		if (app == null) {
			return null;
		}
		return ExpressServerUtils.findProjectForApplication(app);
	}

	public static IApplication findApplicationForServer(IServerAttributes server) {
		try {
			ConnectionURL connectionUrl = ExpressServerUtils.getExpressConnectionUrl(server);
			Connection connection = ConnectionsModelSingleton.getInstance().getConnectionByUrl(connectionUrl);
			String appName = ExpressServerUtils.getExpressApplicationName(server);
			IApplication app = connection == null ? null : connection.getApplicationByName(appName);
			return app;
		} catch (OpenShiftException ose) {
			Logger.error(NLS.bind("Could not find application for server {0}", server.getName()));
			return null;
		}
	}

	public static void updateOpenshiftProjectSettings(IProject project, IApplication app,
			Connection connection, String remoteName, String deployFolder) {
		String qualifier = OpenShiftUIActivator.getDefault().getBundle().getSymbolicName();
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(qualifier);
		node.put(ExpressServerUtils.SETTING_APPLICATION_ID, app.getUUID());
		node.put(ExpressServerUtils.SETTING_APPLICATION_NAME, app.getName());
		setConnectionUrl(connection, node);
		node.put(ExpressServerUtils.SETTING_DOMAIN, app.getDomain().getId());
		node.put(ExpressServerUtils.SETTING_REMOTE_NAME, remoteName);
		if (!StringUtils.isEmpty(deployFolder)) {
			node.put(ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, deployFolder);
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			OpenShiftUIActivator.log(e);
		}
	}

	private static void setConnectionUrl(Connection connection, IEclipsePreferences node) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			node.put(ExpressServerUtils.SETTING_CONNECTIONURL, connectionUrl.toString());
			if (hasUsername(node)) {
				node.put(ExpressServerUtils.SETTING_USERNAME, connection.getUsername());
			}
		} catch (UnsupportedEncodingException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get connection url for connection {0}/{1}",
					connection.getUsername(), connection.getHost()), e);
		} catch (MalformedURLException e) {
			OpenShiftUIActivator.log(NLS.bind("Could not get connection url for connection {0}/{1}",
					connection.getUsername(), connection.getHost()), e);
		}
	}

	private static boolean hasUsername(IEclipsePreferences node) {
		return node.get(ExpressServerUtils.SETTING_USERNAME, null) != null;
	}

	public static IServer setExpressDeployProject(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static IServer setExpressRemoteName(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	/*
	 * Deprecated: These details cannot be changed and are no longer stored in
	 * the server Delete when certain no problems will be caused.
	 */
	@Deprecated
	public static IServer setExpressApplication(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	@Deprecated
	public static IServer setExpressDomain(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DOMAIN, val);
		return wc.save(false, new NullProgressMonitor());
	}

	@Deprecated
	public static IServer setExpressUsername(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_USERNAME, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static int countCommitableChanges(IProject project, IServer server, IProgressMonitor monitor) throws CoreException {
		try {
			Repository repo = EGitUtils.checkedGetRepository(project);
			Set<String> commitable = getCommitableChanges(repo, server, monitor);
			return commitable.size();
		} catch (IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, EGitCoreActivator.PLUGIN_ID, "Unable to count commitable resources", ioe));
		}
	}

	private static Set<String> getCommitableChanges(Repository repo, IServer server, IProgressMonitor monitor)
			throws IOException {
		
		IndexDiff diff = EGitUtils.getIndexChanges(repo, monitor);
		Set<String> set = new HashSet<String>();
		if (diff != null) {
			if (isCommitAddedResources(server))
				set.addAll(diff.getAdded());
			if (isCommitChangedResources(server))
				set.addAll(diff.getChanged());
			if (isCommitConflictingResources(server))
				set.addAll(diff.getConflicting());
			if (isCommitMissingResources(server))
				set.addAll(diff.getMissing());
			if (isCommitModifiedResources(server))
				set.addAll(diff.getModified());
			if (isCommitRemovedResources(server))
				set.addAll(diff.getRemoved());
			if (isCommitUntrackedResources(server))
				set.addAll(diff.getUntracked());
		}
		return set;
	}

	/*
	 * Current behaviour is to commit only: added, changed, modified, removed
	 * 
	 * These can be customized as properties on the server one day, if we wish,
	 * such that each server can have custom settings, or, they can be global
	 * settings
	 */
	public static boolean isCommitAddedResources(IServer server) {
		return true;
	}

	public static boolean isCommitChangedResources(IServer server) {
		return true;
	}

	public static boolean isCommitConflictingResources(IServer server) {
		return false;
	}

	public static boolean isCommitMissingResources(IServer server) {
		return false;
	}

	public static boolean isCommitModifiedResources(IServer server) {
		return true;
	}

	public static boolean isCommitRemovedResources(IServer server) {
		return true;
	}

	public static boolean isCommitUntrackedResources(IServer server) {
		return false;
	}
	
	public static String[] toNames(final IProject[] projects) {
		if (projects == null) {
			return new String[]{};
		}
		String[] names = new String[projects.length];
		for(int i = 0; i < projects.length; i++) {
			names[i] = projects[i].getName();
		}
		return names;
	}

	public static String[] toNames(List<IApplication> apps) {
		if (apps == null) {
			return new String[] {};
		}
		String[] appNames = new String[apps.size()];
		for (int i = 0; i < apps.size(); i++) {
			appNames[i] = apps.get(i).getName();
		}
		return appNames;
	}

	public static void put(Connection connection, TaskModel taskModel) {
		taskModel.putObject(TASK_WIZARD_ATTR_CONNECTION, connection);
	}

	public static Connection getConnection(IServerModeUICallback callback) {
		return (Connection) callback.getAttribute(TASK_WIZARD_ATTR_CONNECTION);
	}

	public static void put(IDomain domain, TaskModel taskModel) {
		taskModel.putObject(TASK_WIZARD_ATTR_DOMAIN, domain);
	}

	public static IDomain getDomain(IServerModeUICallback callback) {
		return (IDomain) callback.getAttribute(TASK_WIZARD_ATTR_DOMAIN);
	}

	public static void put(IApplication application, TaskModel taskModel) {
		taskModel.putObject(ExpressServerUtils.TASK_WIZARD_ATTR_SELECTED_APP, application);
	}
	
	public static IApplication getApplication(IServerModeUICallback callback) {
		return (IApplication) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_SELECTED_APP);
	}
}
