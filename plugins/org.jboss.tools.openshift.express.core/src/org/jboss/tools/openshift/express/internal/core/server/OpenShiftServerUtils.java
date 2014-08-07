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
package org.jboss.tools.openshift.express.internal.core.server;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.core.util.DeployFolder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.osgi.service.prefs.BackingStoreException;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * This class holds the attribute names whose values will be stored inside a
 * server object, as well as the utility methods used to get and set them for a
 * server.
 * 
 * @author Rob Stryker
 */
@SuppressWarnings("restriction")
public class OpenShiftServerUtils {
	
	/* Project settings always had .ui qualifier, so this cannot be changed */
	public static final String QUALIFIER = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$

	
	/* Server Settings */
	public static final String ATTRIBUTE_DEPLOY_PROJECT = "org.jboss.tools.openshift.binary.deployProject"; //$NON-NLS-1$
	public static final String ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS = "org.jboss.tools.openshift.project.override";//$NON-NLS-1$

	/* Legacy Server Settings: Please usage scan before removal */
	public static final String ATTRIBUTE_DEPLOY_PROJECT_LEGACY = "org.jboss.tools.openshift.express.internal.core.server.binary.deployProject";//$NON-NLS-1$
	public static final String ATTRIBUTE_REMOTE_NAME = "org.jboss.tools.openshift.express.internal.core.server.RemoteName";//$NON-NLS-1$
	public static final String ATTRIBUTE_APPLICATION_NAME = "org.jboss.tools.openshift.express.internal.core.server.ApplicationName";//$NON-NLS-1$
	public static final String ATTRIBUTE_APPLICATION_ID = "org.jboss.tools.openshift.express.internal.core.server.ApplicationId";//$NON-NLS-1$
	public static final String ATTRIBUTE_DOMAIN = "org.jboss.tools.openshift.express.internal.core.server.Domain";//$NON-NLS-1$
	public static final String ATTRIBUTE_USERNAME = "org.jboss.tools.openshift.express.internal.core.server.Username";//$NON-NLS-1$
	public static final String ATTRIBUTE_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.express.internal.core.server.DEPLOY_FOLDER_LOC";//$NON-NLS-1$

	/* New Settings inside the project */
	public static final String SETTING_REMOTE_NAME = "org.jboss.tools.openshift.RemoteName";//$NON-NLS-1$
	public static final String SETTING_APPLICATION_NAME = "org.jboss.tools.openshift.ApplicationName";//$NON-NLS-1$
	public static final String SETTING_APPLICATION_ID = "org.jboss.tools.openshift.ApplicationId";//$NON-NLS-1$
	public static final String SETTING_DOMAIN_ID = "org.jboss.tools.openshift.Domain";//$NON-NLS-1$
	public static final String SETTING_USERNAME = "org.jboss.tools.openshift.Username";//$NON-NLS-1$
	public static final String SETTING_CONNECTIONURL = "org.jboss.tools.openshift.Connection";//$NON-NLS-1$
	public static final String SETTING_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.DeployFolder";//$NON-NLS-1$

	// Legacy, not to be used
	// public static final String ATTRIBUTE_PASSWORD =
	// "org.jboss.tools.openshift.express.internal.core.server.Password";
	public static final String ATTRIBUTE_REMOTE_NAME_DEFAULT = "origin";//$NON-NLS-1$
	private static final String ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT = "deployments";//$NON-NLS-1$

	public static final String PREFERENCE_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.express.internal.core.server.IgnoreContextRoot";//$NON-NLS-1$

	/** the OpensHift Server Type as defined in the plugin.xml. */
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.express.openshift.server.type";//$NON-NLS-1$

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
		String qualifier = QUALIFIER;
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
		final String appName = getApplicationName(server);
		if (StringUtils.isEmpty(appName)) {
			return null;
		}
		final ConnectionURL connectionUrl = getConnectionUrl(server);
		if (connectionUrl == null) {
			return null;
		}
		try {
			Connection connection = ConnectionsModelSingleton.getInstance().getConnectionByUrl(connectionUrl);
			if (connection == null) {
				OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not find connection {0}", connectionUrl.toString()));
				return null;
			}
			IDomain domain = connection.getDomain(getDomainName(server));
			return connection.getApplication(appName, domain);
		} catch (OpenShiftException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Failed to retrieve application ''{0}'' at url ''{1}}'", appName, connectionUrl), e);
			return null;
		}
	}

	/* Settings stored only in the project */
	public static String getApplicationName(IServerAttributes attributes) {
		return getProjectAttribute(
				getDeployProject(attributes), 
				SETTING_APPLICATION_NAME,	
				attributes.getAttribute(ATTRIBUTE_APPLICATION_NAME, (String) null));
	}

	public static String getApplicationId(IServerAttributes attributes) {
		return getProjectAttribute(getDeployProject(attributes), SETTING_APPLICATION_ID,
				attributes.getAttribute(ATTRIBUTE_APPLICATION_ID, (String) null));
	}

	public static String getDomainName(IServerAttributes attributes) {
		return getProjectAttribute(getDeployProject(attributes), SETTING_DOMAIN_ID,
				attributes.getAttribute(ATTRIBUTE_DOMAIN, (String) null));
	}

	private static String getUsername(IServerAttributes attributes) {
		return getProjectAttribute(getDeployProject(attributes), SETTING_USERNAME,
				attributes.getAttribute(ATTRIBUTE_USERNAME, (String) null));
	}

	public static ConnectionURL getConnectionUrl(IServerAttributes attributes) {
		try {
			String connectionUrlString = getProjectAttribute(
					getDeployProject(attributes), SETTING_CONNECTIONURL, null);
			if (!StringUtils.isEmpty(connectionUrlString)) {
				return ConnectionURL.forURL(connectionUrlString);
			}
			
			String username = getUsername(attributes);
			if (!StringUtils.isEmpty(username)) {
				return ConnectionURL.forUsername(username);
			}
		} catch (UnsupportedEncodingException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
		} catch (MalformedURLException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
		}

		return null;
	}
		
	public static String getDeployFolder(IServerAttributes attributes) {
		return getDeployFolder(attributes, getApplication(attributes));
	}

	public static String getDeployFolder(IServerAttributes attributes, IApplication application) {
		return getDeployFolder(attributes, getDefaultDeployFolder(application));
	}
		
	/* Settings stored in the project, maybe over-ridden in the server */
	private static String getDeployFolder(IServerAttributes attributes, String defaultDeployFolder) {
		if (getOverridesProject(attributes)) {
			return attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, defaultDeployFolder);
		}
		
		return getProjectAttribute(
				getDeployProject(attributes), SETTING_DEPLOY_FOLDER_NAME, defaultDeployFolder);
	}

	public static String getDefaultDeployFolder(IServerAttributes server) {
		return getDefaultDeployFolder(getApplication(server));
	}
	
	
	public static String getDefaultDeployFolder(IApplication application) {
		Assert.isNotNull(application);
		DeployFolder deployFolder = DeployFolder.getByCartridgeName(application.getCartridge().getName());
		if (deployFolder == null) {
			return null;
		}
		return deployFolder.getDeployFolder();
	}
		
	public static IContainer getContainer(String name, IProject project) {
		if (!StringUtils.isEmpty(name)) {
			return (IContainer) project.findMember(new Path(name));
		} else {
			return project;
		}

	}
	
	public static String getRemoteName(IServerAttributes attributes) {
		if (getOverridesProject(attributes))
			return attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, ATTRIBUTE_REMOTE_NAME_DEFAULT);
		return getProjectAttribute(getDeployProject(attributes), 
				SETTING_REMOTE_NAME, ATTRIBUTE_REMOTE_NAME_DEFAULT);
	}

	public static String getDeployFolder(IServerAttributes attributes, int fromWhere) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, (String) null);
		if (fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getDeployProject(attributes), SETTING_DEPLOY_FOLDER_NAME, null);
		if (fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if (getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_DEPLOY_FOLDER_JBOSS_DEFAULT : fromProject;
	}

	public static String getRemoteName(IServerAttributes attributes, int fromWhere) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, (String) null);
		if (fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getDeployProject(attributes), SETTING_REMOTE_NAME, null);
		if (fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if (getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromProject;
	}

	/* Settings stored only in the server */
	public static String getDeployProjectName(IServerAttributes attributes) {
		return attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT,
				attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT_LEGACY, (String) null));
	}

	public static IProject getDeployProject(IServerAttributes attributes) {
		String name = getDeployProjectName(attributes);
		if (StringUtils.isEmpty(name)) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
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

	public static IServer fillServerWithOpenShiftDetails(IServer server, String deployProject, String remote,
			String serverName, IApplication application, IDomain domain) throws CoreException {
		ServerWorkingCopy wc = (ServerWorkingCopy) server.createWorkingCopy();
		String deployFolder = getDefaultDeployFolder(application);
		String host = getHost(application);
		String applicationName = getApplicationName(application);
		String domainId = getDomainId(domain);
		fillServerWithOpenShiftDetails((IServerWorkingCopy) wc, serverName,
				host, deployProject, deployFolder, remote, applicationName, domainId);
		IServer saved = wc.save(true, new NullProgressMonitor());
		return saved;
	}

	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, String serverName,  
			IProject deployProject, String deployFolder, String remote, IApplication application, IDomain domain) {
		String host = getHost(application);
		String deployProjectName = getDeployProjectName(deployProject);
		String applicationName = getApplicationName(application);
		String domainId = getDomainId(domain);
		fillServerWithOpenShiftDetails(wc, serverName, host, deployProjectName, deployFolder, remote, applicationName, domainId);
	}

	private static String getDeployProjectName(IProject deployProject) {
		String deployProjectName = null;
		if (deployProject != null) {
			deployProjectName = deployProject.getName();
		}
		return deployProjectName;
	}

	private static String getHost(IApplication application) {
		String host = null;
		if (application != null) {
			host = application.getApplicationUrl();
		}
		return host;
	}

	private static String getApplicationName(IApplication application) {
		String name = null;
		if (application != null) {
			name = application.getName();
		}
		return name;
	}

	private static String getDomainId(IDomain domain) {
		String id = null;
		if (domain != null) {
			id = domain.getId();
		}
		return id;
	}

	/**
	 * Fills the given settings into the given server adapter working copy.
	 * <b>IMPORTANT:</b> If the server adapter name is matching an existing server adapter, then
	 * we're updating this existing server adapter. If the name is a new one, then we're
	 * creating a new server adapter.
	 * 
	 * @param wc
	 *            the server adapter working copy to configure
	 * @param serverName
	 *            the name for the server adapter
	 * @param host
	 *            the host for the server adapter
	 * @param deployProject
	 *            the deploy project for the server adapter
	 * @param deployFolder
	 *            the deploy folder for the server adapter
	 * @param remote
	 *            the remote for the server adapter
	 * @param applicationName
	 *            the application name for the server adapter
	 */
	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, String serverName, String host,
			String deployProject, String deployFolder, String remote, String applicationName, String domainName) {
		wc.setHost(trimHost(host));
		wc.setAttribute(IDeployableServer.SERVER_MODE, OpenShiftServer.OPENSHIFT_MODE_ID);
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, deployProject);
		// wc.setAttribute(ATTRIBUTE_USERNAME, username);
		wc.setAttribute(ATTRIBUTE_DOMAIN, domainName);
		wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, applicationName);
		// wc.setAttribute(ATTRIBUTE_APPLICATION_ID, appId);
		 wc.setAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, deployFolder);
		// wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, mode);
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, remote);
		((ServerWorkingCopy) wc).setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		wc.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, String.valueOf(Boolean.TRUE));
		wc.setAttribute(IJBossToolingConstants.WEB_PORT, 80);
		wc.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, "false");
		wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
		wc.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
		wc.setName(serverName);
	}
	
	public static String getDefaultServerName(IApplication application) {
		if (application == null) {
			return null;
		}
		return getDefaultServerName(application.getName());
	}

	public static String getDefaultServerName(String baseName) {
		return ServerUtil.getDefaultServerName(baseName + " at OpenShift");
	}
	
	public static boolean isDefaultName(String serverName) {
		if (StringUtils.isEmpty(serverName)) {
			return false;
		}

		return serverName.startsWith(OpenShiftServer.DEFAULT_SERVER_NAME_BASE);
	}
	
	protected static String trimHost(String url) {
		if (StringUtils.isEmpty(url)) {
			return url;
		}
		if (url.indexOf("://") != -1)
			url = url.substring(url.indexOf("://") + 3);
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		return url;
	}

	public static IServer createServer(String serverID) throws CoreException {
		return createServer(ServerCore.findServerType(serverID), serverID);
	}

	public static IServer createServer(IServerType serverType, String serverName)
			throws CoreException {
		IServerWorkingCopy serverWC = serverType.createServer(null, null,
				new NullProgressMonitor());
		serverWC.setRuntime(null);
		serverWC.setName(serverName);
		serverWC.setServerConfiguration(null);
		serverWC.setAttribute(IDeployableServer.SERVER_MODE, OpenShiftServer.OPENSHIFT_MODE_ID);
		return serverWC.save(true, new NullProgressMonitor());
	}

	/**
	 * Returns true if the given server is an OpenShift one, false otherwise.
	 * 
	 * @param server
	 *            the server adapter to check
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
	 *            the server adapter to check
	 * @return true or false
	 */
	public static boolean isInOpenshiftBehaviourMode(IServer server) {
		String mode = server.getAttribute(IDeployableServer.SERVER_MODE, (String)null);
		if (OpenShiftServer.OPENSHIFT_MODE_ID.equals(mode))
			return true;
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
		final List<IProject> results = new ArrayList<IProject>();
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
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not look up remotes for project {0}", project), ce);
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
		String domain = getProjectAttribute(project, SETTING_DOMAIN_ID, null);
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

	public static void updateOpenshiftProjectSettings(IProject project, IApplication app,
			IDomain domain, Connection connection, String remoteName, String deployFolder) {
		String qualifier = QUALIFIER;
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(qualifier);
		node.put(OpenShiftServerUtils.SETTING_APPLICATION_ID, app.getUUID());
		node.put(OpenShiftServerUtils.SETTING_APPLICATION_NAME, app.getName());
		node.put(OpenShiftServerUtils.SETTING_DOMAIN_ID, app.getDomain().getId());
		setConnectionUrl(connection, node);
		node.put(OpenShiftServerUtils.SETTING_REMOTE_NAME, remoteName);
		if (!StringUtils.isEmpty(deployFolder)) {
			node.put(OpenShiftServerUtils.SETTING_DEPLOY_FOLDER_NAME, deployFolder);
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			OpenShiftCoreActivator.pluginLog().logError(e);
		}
	}

	private static void setConnectionUrl(Connection connection, IEclipsePreferences node) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			node.put(OpenShiftServerUtils.SETTING_CONNECTIONURL, connectionUrl.toString());
			if (hasUsername(node)) {
				node.put(OpenShiftServerUtils.SETTING_USERNAME, connection.getUsername());
			}
		} catch (UnsupportedEncodingException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for connection {0}/{1}",
					connection.getUsername(), connection.getHost()), e);
		} catch (MalformedURLException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for connection {0}/{1}",
					connection.getUsername(), connection.getHost()), e);
		}
	}

	private static boolean hasUsername(IEclipsePreferences node) {
		return node.get(OpenShiftServerUtils.SETTING_USERNAME, null) != null;
	}

	public static IServer setDeployProject(IServer server, String val) throws CoreException {
		IServerWorkingCopy wc = server.createWorkingCopy();
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, val);
		return wc.save(false, new NullProgressMonitor());
	}

	public static IServer setRemoteName(IServer server, String val) throws CoreException {
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
}
