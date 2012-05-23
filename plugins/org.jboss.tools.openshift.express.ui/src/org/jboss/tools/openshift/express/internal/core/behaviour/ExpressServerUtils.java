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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethodType;
import org.jboss.ide.eclipse.as.core.util.DeploymentPreferenceLoader;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.core.util.RuntimeUtils;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftPasswordStorageKey;
import org.osgi.service.prefs.BackingStoreException;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * This class holds the attribute names whose values will be
 * stored inside a server object, as well as the utility methods
 * used to get and set them for a server. 
 *
 * @author Rob Stryker
 */
@SuppressWarnings("restriction")
public class ExpressServerUtils {
	/* Server Settings */
	public static final String ATTRIBUTE_DEPLOY_PROJECT =  "org.jboss.tools.openshift.binary.deployProject";
	public static final String ATTRIBUTE_OVERRIDE_PROJECT_SETTINGS =  "org.jboss.tools.openshift.project.override";
	
	/* Legacy Server Settings: Please usage scan before removal */
	public static final String ATTRIBUTE_DEPLOY_PROJECT_LEGACY =  "org.jboss.tools.openshift.express.internal.core.behaviour.binary.deployProject";
	public static final String ATTRIBUTE_REMOTE_NAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.RemoteName";
	public static final String ATTRIBUTE_APPLICATION_NAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationName";
	public static final String ATTRIBUTE_APPLICATION_ID =  "org.jboss.tools.openshift.express.internal.core.behaviour.ApplicationId";
	public static final String ATTRIBUTE_DOMAIN =  "org.jboss.tools.openshift.express.internal.core.behaviour.Domain";
	public static final String ATTRIBUTE_USERNAME =  "org.jboss.tools.openshift.express.internal.core.behaviour.Username";
	public static final String ATTRIBUTE_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.express.internal.core.behaviour.DEPLOY_FOLDER_LOC";

	/* New Settings inside the project */
	public static final String SETTING_REMOTE_NAME =  "org.jboss.tools.openshift.RemoteName";
	public static final String SETTING_APPLICATION_NAME =  "org.jboss.tools.openshift.ApplicationName";
	public static final String SETTING_APPLICATION_ID =  "org.jboss.tools.openshift.ApplicationId";
	public static final String SETTING_DOMAIN =  "org.jboss.tools.openshift.Domain";
	public static final String SETTING_USERNAME =  "org.jboss.tools.openshift.Username";
	public static final String SETTING_DEPLOY_FOLDER_NAME = "org.jboss.tools.openshift.DeployFolder";

	// Legacy, not to be used
	//public static final String ATTRIBUTE_PASSWORD =  "org.jboss.tools.openshift.express.internal.core.behaviour.Password";
	public static final String ATTRIBUTE_REMOTE_NAME_DEFAULT =  "origin";
	public static final String ATTRIBUTE_DEPLOY_FOLDER_DEFAULT = "deployments";
	
	public static final String PREFERENCE_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.express.internal.core.behaviour.IgnoreContextRoot";
	
	/** the OpensHift Server Type as defined in the plugin.xml.*/
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.express.openshift.server.type";
	
	/* For use inside express wizard fragment */
	public static final String TASK_WIZARD_ATTR_USER = "user";
	public static final String TASK_WIZARD_ATTR_DOMAIN = "domain";
	public static final String TASK_WIZARD_ATTR_APP_LIST = "appList";
	public static final String TASK_WIZARD_ATTR_SELECTED_APP = "application";
	
	/* For use in finding not just the effective value, but values stored either in project or server */
	public static int SETTING_FROM_PROJECT = 1;
	public static int SETTING_FROM_SERVER = 2;
	public static int SETTING_EFFECTIVE_VALUE = 3;
	
	public static String getProjectAttribute(IProject project, String attributeName, String defaultVal) {
		if( project == null )
			return defaultVal;
		String qualifier = OpenShiftUIActivator.getDefault().getBundle().getSymbolicName();
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(qualifier);
		return node.get(attributeName, defaultVal);
	}

	/* Settings stored only in the project */
	public static String getExpressApplicationName(IServerAttributes attributes ) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_APPLICATION_NAME, 
				attributes.getAttribute(ATTRIBUTE_APPLICATION_NAME, (String)null));
	}

	public static String getExpressApplicationId(IServerAttributes attributes ) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_APPLICATION_ID, 
				attributes.getAttribute(ATTRIBUTE_APPLICATION_ID, (String)null));
	}

	public static String getExpressDomain(IServerAttributes attributes ) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_DOMAIN, 
				attributes.getAttribute(ATTRIBUTE_DOMAIN, (String)null));
	}

	public static String getExpressUsername(IServerAttributes attributes ) {
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_USERNAME, 
				attributes.getAttribute(ATTRIBUTE_USERNAME, (String)null));
	}

	
	/* Settings stored in the project, maybe over-ridden in the server */
	public static String getExpressDeployFolder(IServerAttributes attributes ) {
		if( getOverridesProject(attributes)) 
			return attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_DEPLOY_FOLDER_NAME,
				ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
	}
	
	public static String getExpressRemoteName(IServerAttributes attributes ) {
		if( getOverridesProject(attributes)) 
			return attributes.getAttribute(ATTRIBUTE_REMOTE_NAME, ATTRIBUTE_REMOTE_NAME_DEFAULT);
		return getProjectAttribute(getExpressDeployProject2(attributes), SETTING_REMOTE_NAME,
				ATTRIBUTE_REMOTE_NAME_DEFAULT);
	}

	public static String getExpressDeployFolder(IServerAttributes attributes, int fromWhere ) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME,(String)null);
		if( fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getExpressDeployProject2(attributes), SETTING_DEPLOY_FOLDER_NAME, null);
		if( fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if( getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_DEPLOY_FOLDER_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_DEPLOY_FOLDER_DEFAULT : fromProject;
	}
	
	public static String getExpressRemoteName(IServerAttributes attributes, int fromWhere ) {
		String fromServer = attributes.getAttribute(ATTRIBUTE_REMOTE_NAME,(String)null);
		if( fromWhere == SETTING_FROM_SERVER)
			return fromServer;
		String fromProject = getProjectAttribute(getExpressDeployProject2(attributes), SETTING_REMOTE_NAME, null);
		if( fromWhere == SETTING_FROM_PROJECT)
			return fromProject;
		if( getOverridesProject(attributes))
			return fromServer == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromServer;
		return fromProject == null ? ATTRIBUTE_REMOTE_NAME_DEFAULT : fromProject;
	}

	/* Settings stored only in the server */
	public static String getExpressDeployProject(IServerAttributes attributes ) {
		return attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT, 
				attributes.getAttribute(ATTRIBUTE_DEPLOY_PROJECT_LEGACY, (String)null));
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
			String deployProject, String remote) throws CoreException {
		ServerWorkingCopy wc = (ServerWorkingCopy)server.createWorkingCopy();
		fillServerWithOpenShiftDetails((IServerWorkingCopy)wc, host, deployProject,remote);
		IServer saved = wc.save(true, new NullProgressMonitor());
		return saved;
	}
	
	public static void fillServerWithOpenShiftDetails(IServerWorkingCopy wc, String host, 
			String deployProject, String remote)  {

		if( host != null ) {
			if( host.indexOf("://") != -1)
				host = host.substring(host.indexOf("://") + 3);
			if( host.endsWith("/"))
				host = host.substring(0, host.length()-1);
		}
		wc.setHost(host);
		wc.setAttribute(IDeployableServer.SERVER_MODE, ExpressBehaviourDelegate.OPENSHIFT_ID);
		wc.setAttribute(ATTRIBUTE_DEPLOY_PROJECT, deployProject);
//		wc.setAttribute(ATTRIBUTE_USERNAME, username);
//		wc.setAttribute(ATTRIBUTE_DOMAIN, domain);
//		wc.setAttribute(ATTRIBUTE_APPLICATION_NAME, appName);
//		wc.setAttribute(ATTRIBUTE_APPLICATION_ID, appId);
//		wc.setAttribute(ATTRIBUTE_DEPLOY_FOLDER_NAME, projectRelativeFolder);
//		wc.setAttribute(ATTRIBUTE_EXPRESS_MODE, mode);
		wc.setAttribute(ATTRIBUTE_REMOTE_NAME, remote);
		((ServerWorkingCopy)wc).setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		wc.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, "true");
		wc.setAttribute(IJBossToolingConstants.WEB_PORT, 80);
		wc.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, "false");
		wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
		wc.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
	}
	
	
	public static IServer createServerAndRuntime(String runtimeID, String serverID,
			String location, String configuration) throws CoreException {
		IRuntime runtime = RuntimeUtils.createRuntime(runtimeID, location, configuration);
		return createServer(runtime, serverID);
	}
	
	public static IServer createServer(IRuntime runtime, String serverID) throws CoreException {
		return createServer2(runtime, ServerCore.findServerType(serverID), serverID);
	}
	
	public static IServer createServer(IRuntime runtime, IServerType serverType, String serverName) throws CoreException {
		return createServer2(runtime, serverType, serverName);
	}
	
	public static IServer createServer2(IRuntime currentRuntime, IServerType serverType, String serverName) throws CoreException {
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
	 * @param server the server to check
	 * @return true or false
	 */
	public static boolean isOpenShiftRuntime(IServerAttributes server) {
		final String serverTypeId = server.getServerType().getId();
		return (OPENSHIFT_SERVER_TYPE.equals(serverTypeId));
	}

	/**
	 * Returns true if the given server is a server using an openshift behaviour
	 * @param server the server to check
	 * @return true or false
	 */
	public static boolean isInOpenshiftBehaviourMode(IServer server) {
		IDeployableServer ds = ServerConverter.getDeployableServer(server);
		if( ds != null ) {
			IJBossServerPublishMethodType type = DeploymentPreferenceLoader.getCurrentDeploymentMethodType(server);
			if( type != null ) {
				String id = type.getId();
				if( ExpressBinaryBehaviourDelegate.OPENSHIFT_BINARY_ID.equals(id) || ExpressBehaviourDelegate.OPENSHIFT_ID.equals(id))
					return true;
			}
		}
		return false;
	}

	public static IApplication findApplicationForProject(IProject p, List<IApplication> applications) 
			throws OpenShiftException, CoreException {
		List<URIish> uris = EGitUtils.getRemoteURIs(p);
		Iterator<IApplication> i = applications.iterator();
		while(i.hasNext()) {
			IApplication a = i.next();
			String gitUri = a.getGitUrl();
			Iterator<URIish> j = uris.iterator();
			while(j.hasNext()) {
				String projUri = j.next().toPrivateString();
				if( projUri.equals(gitUri)) {
					return a;
				}
			}
		}
		return null;
	}

	/**
	 * This method will search available projects for one that has a 
	 * git remote URI that matches that of this IApplication
	 * 
	 * @param application
	 * @return
	 */
	public static IProject[] findProjectsForApplication(final IApplication application) {
		final ArrayList<IProject> results = new ArrayList<IProject>();
		if( application ==null )
			return null;
		final String gitUri = application.getGitUrl();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for( int i = 0; i < projects.length; i++ ) {
			List<URIish> uris = null;
			try {
				uris = EGitUtils.getRemoteURIs(projects[i]);
				Iterator<URIish> it = uris.iterator();
				while(it.hasNext()) {
					String projURI = it.next().toPrivateString();
					if( projURI.equals(gitUri))
						results.add(projects[i]);
				}
			} catch(CoreException ce) {
				// Log? Not 100 required, just skip this project?
			}
		}
		return results.toArray(new IProject[results.size()]);
	}
	
	/**
	 * This method will search for all projects connected to git
	 * and having the proper settings file containing 
	 * domain, application id, app name, and username
	 * 
	 * @return
	 */
	public static IProject[] findAllSuitableOpenshiftProjects() {
		final ArrayList<IProject> results = new ArrayList<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for( int i = 0; i < projects.length; i++ ) {
			if( EGitUtils.getRepository(projects[i]) != null ) {
				String appName = getProjectAttribute(projects[i], SETTING_APPLICATION_NAME, null);
				String appId = getProjectAttribute(projects[i], SETTING_APPLICATION_ID, null);
				String domain = getProjectAttribute(projects[i], SETTING_DOMAIN, null);
				String username = getProjectAttribute(projects[i], SETTING_USERNAME, null);
				if( appName != null && appId != null && domain != null && username != null ) {
					results.add(projects[i]);
				}
			}
		}
		return results.toArray(new IProject[results.size()]);
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
			String user = ExpressServerUtils.getExpressUsername(server);
			UserDelegate user2 = UserModel.getDefault().findUser(user);
			String appName = ExpressServerUtils.getExpressApplicationName(server);
			IApplication app = user2 == null ? null : user2.getApplicationByName(appName);
			return app;
		} catch(OpenShiftException ose) {
			Logger.error(NLS.bind("Could not find application for server {0}", server.getName()));
			return null;
		} catch(SocketTimeoutException ose) {
			Logger.error(NLS.bind("Could not find application for server {0}", server.getName()));
			return null;
		}
	}
	
	public static void updateOpenshiftProjectSettings(IProject project, IApplication app, 
			UserDelegate user, String remoteName, String deployFolder) {
		String qualifier = OpenShiftUIActivator.getDefault().getBundle().getSymbolicName();
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences node = context.getNode(qualifier);
		node.put(ExpressServerUtils.SETTING_APPLICATION_ID, app.getUUID());
		node.put(ExpressServerUtils.SETTING_APPLICATION_NAME, app.getName());
		node.put(ExpressServerUtils.SETTING_USERNAME, user.getUsername());
		node.put(ExpressServerUtils.SETTING_DOMAIN, app.getDomain().getId());
		node.put(ExpressServerUtils.SETTING_REMOTE_NAME, remoteName);
		node.put(ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, deployFolder);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			OpenShiftUIActivator.log(e);
		}
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

	
	/* Deprecated: These details cannot be changed and are no longer stored in the server
	 * Delete when certain no problems will be caused. 
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
}
