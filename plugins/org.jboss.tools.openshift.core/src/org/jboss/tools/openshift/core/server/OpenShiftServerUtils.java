/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.service.prefs.BackingStoreException;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerUtils {

	private static final String LIVERELOAD_PORT_KEY = "port";//Key to the port # of the host the LiveReload server need to proxy

	public static final String SERVER_PROJECT_QUALIFIER = "org.jboss.tools.openshift.core"; //$NON-NLS-1$

	public static final String ATTR_SERVICE = "org.jboss.tools.openshift.Service"; //$NON-NLS-1$
	public static final String ATTR_DEPLOYPROJECT = "org.jboss.tools.openshift.DeployProject"; //$NON-NLS-1$
	public static final String ATTR_SOURCE_PATH = "org.jboss.tools.openshift.SourcePath"; //$NON-NLS-1$
	public static final String ATTR_POD_PATH = "org.jboss.tools.openshift.PodPath"; //$NON-NLS-1$

	public static final String ATTR_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.IgnoreContextRoot";//$NON-NLS-1$
	public static final String ATTR_OVERRIDE_PROJECT_SETTINGS = "org.jboss.tools.openshift.project.Override";//$NON-NLS-1$
	
	public static final String ATTR_CONNECTIONURL = "org.jboss.tools.openshift.Connection";//$NON-NLS-1$
	
	/** the OpensHift Server Type as defined in the plugin.xml. */
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.openshift.server.type";//$NON-NLS-1$

	public static String getServerName(IService service, Connection connection) {
		if (service == null) {
			return null;
		}

		return new StringBuilder(service.getName())
				.append(" at OpenShift 3 (")
				.append(UrlUtils.cutPort(UrlUtils.cutScheme(connection.getHost())))
				.append(")")
				.toString();
	}

	public static void updateServer(String serverName, String connectionUrl, IService service, String podPath, String sourcePath, IProject deployProject, IServerWorkingCopy server) {
		String host = getHost(service);
		String deployProjectName = ProjectUtils.getName(deployProject);
		updateServer(serverName, host, connectionUrl, deployProjectName, OpenShiftResourceUniqueId.get(service), sourcePath, podPath, server);
	}

	private static String getHost(IService service) {
		if (service == null) {
			return null;
		}

		String host = null;

		com.openshift.restclient.model.IProject project = service.getProject();
		if (project != null) {

			//TODO Ideally, we should use a route selected during the Server creation, in case there are several
			//Until then, we'll fall back on always choosing the 1st route to open a browser
			IRoute route = getRoute(project.getResources(ResourceKind.ROUTE));
			if (route != null) {
				host = route.getURL();
			}
		}
		if (host == null) {
			host = service.getPortalIP();
		}
		return host;
	}

	private static IRoute getRoute(List<IRoute> routes) {
		return routes == null || routes.isEmpty()? null : routes.get(0);
	}

	/**
	 * Fills the given settings into the given server adapter working copy.
	 * <b>IMPORTANT:</b> If the server adapter name is matching an existing server adapter, then
	 * we're updating this existing server adapter. If the name is a new one, then we're
	 * creating a new server adapter.
	 * 
	 * @param server
	 *            the server adapter working copy to configure
	 * @param serverName
	 *            the name for the server adapter
	 * @param host
	 *            the host for the server adapter
	 * @param deployProjectName
	 *            the deploy project for the server adapter
	 * @param deployFolder
	 *            the deploy folder for the server adapter
	 * @param remote
	 *            the remote for the server adapter
	 * @param applicationName
	 *            the application name for the server adapter
	 */
	public static void updateServer(String serverName, String host, String connectionUrl, String deployProjectName, String serviceId, String sourcePath, String podPath, IServerWorkingCopy server) {
		updateServer(server);

		server.setName(serverName);
		server.setHost(UrlUtils.getHost(host));

		server.setAttribute(ATTR_CONNECTIONURL, connectionUrl);
		server.setAttribute(ATTR_DEPLOYPROJECT, deployProjectName);
		server.setAttribute(ATTR_SOURCE_PATH, sourcePath);
		server.setAttribute(ATTR_POD_PATH, podPath);
		server.setAttribute(ATTR_SERVICE, serviceId);
	}

	private static void updateServer(IServerWorkingCopy server) {
		server.setAttribute(IDeployableServer.SERVER_MODE, OpenShiftServer.OPENSHIFT3_MODE_ID);
		((ServerWorkingCopy) server).setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
		server.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, String.valueOf(Boolean.TRUE));
		int webPort = 80;//TODO should we determine the webPort from the route?
		server.setAttribute(IJBossToolingConstants.WEB_PORT, webPort);
		server.setAttribute(LIVERELOAD_PORT_KEY, webPort);//So that we can open via LiveReload
		server.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, Boolean.FALSE.toString());
		server.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
		server.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
	}
	
	public static void updateServerProject(String connectionUrl, IService service, String sourcePath, String podPath, IProject project) {
		updateServerProject(connectionUrl, OpenShiftResourceUniqueId.get(service), sourcePath, podPath, project);
	}

	public static void updateServerProject(String connectionUrl, String serviceId, String sourcePath, String podPath, IProject project) {
		IEclipsePreferences node = ServerUtils.getProjectNode(SERVER_PROJECT_QUALIFIER, project);
		node.put(ATTR_CONNECTIONURL, connectionUrl);
		node.put(ATTR_DEPLOYPROJECT, project.getName());
		node.put(ATTR_SOURCE_PATH, sourcePath);
		node.put(ATTR_POD_PATH, podPath);
		node.put(ATTR_SERVICE, serviceId);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			OpenShiftCoreActivator.pluginLog().logError(e);
		}
	}

	public static IProject getDeployProject(IServerAttributes attributes) {
		// TODO: implement override project settings with server settings
		return ProjectUtils.getProject(getDeployProjectName(attributes));
	}

	public static String getDeployProjectName(IServerAttributes attributes) {
		if (attributes == null) {
			return null;
		}
		return attributes.getAttribute(ATTR_DEPLOYPROJECT, (String) null);
	}

	public static boolean isIgnoresContextRoot(IServerAttributes server) {
		return server.getAttribute(ATTR_IGNORE_CONTEXT_ROOT, true);
	}

	/**
	 * Returns true if the given server is an OpenShift one, false otherwise.
	 * 
	 * @param server
	 *            the server adapter to check
	 * @return true or false
	 */
	public static boolean isOpenShiftRuntime(IServerAttributes server) {
		return OPENSHIFT_SERVER_TYPE.equals(server.getServerType().getId());
	}

//	public static ConnectionURL getConnectionUrl(IServerAttributes attributes) {
//		try {
//			String connectionUrlString = getProjectAttribute(
//					PROJECTATTR_CONNECTIONURL, null, getDeployProject(attributes));
//			if (!StringUtils.isEmpty(connectionUrlString)) {
//				return ConnectionURL.forURL(connectionUrlString);
//			}
//			
//		} catch (UnsupportedEncodingException e) {
//			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
//		} catch (MalformedURLException e) {
//			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
//		}
//
//		return null;
//	}

	public static Connection getConnection(IServerAttributes attributes) {
		try {
			String url = getProjectAttribute(
					ATTR_CONNECTIONURL, null, getDeployProject(attributes));
			if (!StringUtils.isEmpty(url)) {
				ConnectionURL connectionUrl = ConnectionURL.forURL(url);
				return ConnectionsRegistrySingleton.getInstance().getByUrl(connectionUrl, Connection.class);
 			}
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not get connection url for user {0}", attributes.getName()), e);
		}

		return null;
	}

	public static IService getService(IServerAttributes attributes) {
		return getService(attributes, getConnection(attributes));
	}
	
	public static IService getService(IServerAttributes attributes, Connection connection) {
		// TODO: implement override project settings with server settings
		String uniqueId = getProjectAttribute(ATTR_SERVICE, null, getDeployProject(attributes));
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}
		
		return OpenShiftResourceUniqueId.getByUniqueId(uniqueId, 
				connection.getResources(ResourceKind.SERVICE, 
						OpenShiftResourceUniqueId.getProject(uniqueId)));
	}

	public static String getPodPath(IServerAttributes attributes) {
		// TODO: implement override project settings with server settings
		return getProjectAttribute(ATTR_POD_PATH, null, getDeployProject(attributes));
	}
	
	public static String getSourcePath(IServerAttributes attributes) {
		// TODO: implement override project settings with server settings
		String rawSourcePath = getProjectAttribute(ATTR_SOURCE_PATH, null, getDeployProject(attributes));
		if (org.apache.commons.lang.StringUtils.isBlank(rawSourcePath)) {
			return rawSourcePath;
		}
		String path = VariablesHelper.replaceVariables(rawSourcePath);
		return path;
	}

	public static boolean isOverridesProject(IServerAttributes server) {
		return server.getAttribute(ATTR_OVERRIDE_PROJECT_SETTINGS, false);
	}

	public static String getProjectAttribute(String name, String defaultValue, IServerAttributes attributes) {
		return getProjectAttribute(name, defaultValue, getDeployProject(attributes));
	}

	/**
	 * Returns the attribute value for the given name and project. The given
	 * default value is return if the value doesnt exist or cannot be retrieved.
	 * 
	 * @param project
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getProjectAttribute(String name, String defaultValue, IProject project) {
		return ServerUtils.getProjectAttribute(name, defaultValue, SERVER_PROJECT_QUALIFIER, project);
	}
}
