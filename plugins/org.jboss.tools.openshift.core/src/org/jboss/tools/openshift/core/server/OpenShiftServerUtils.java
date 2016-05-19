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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModule;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.osgi.service.prefs.BackingStoreException;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

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
	public static final String ATTR_ROUTE = "org.jboss.tools.openshift.Route"; //$NON-NLS-1$

	public static final String ATTR_IGNORE_CONTEXT_ROOT = "org.jboss.tools.openshift.IgnoreContextRoot";//$NON-NLS-1$
	public static final String ATTR_OVERRIDE_PROJECT_SETTINGS = "org.jboss.tools.openshift.project.Override";//$NON-NLS-1$

	public static final String ATTR_CONNECTIONURL = "org.jboss.tools.openshift.Connection";//$NON-NLS-1$
	
	/** the OpensHift Server Type as defined in the plugin.xml. */
	public static final String OPENSHIFT_SERVER_TYPE = "org.jboss.tools.openshift.openshift.server.type";//$NON-NLS-1$

	public static final String SERVER_START_ON_CREATION = "org.jboss.tools.openshift.SERVER_START_ON_CREATION";

	private static final Collection<String> EAP_LIKE_KEYWORDS = Collections.unmodifiableCollection(Arrays.asList("eap", "wildfly"));

	public static IServer findServerForService(String serviceName) {
		final IServerType serverType = getServerType();
		return Stream.of(ServerCore.getServers())
				.filter(server -> server.getServerType()
						.equals(serverType)
						&& server.getAttribute(OpenShiftServerUtils.ATTR_SERVICE, "").equals(serviceName))
				.findFirst().orElse(null);
	}
	
	public static IServerType getServerType() {
		return ServerCore.findServerType(OpenShiftServer.SERVER_TYPE_ID);
	}
	
	public static String getServerName(IService service, IConnection connection) {
		if (service == null) {
			return null;
		}

		String baseName = new StringBuilder(service.getName())
				.append(" at OpenShift 3 (")
				.append(UrlUtils.cutPort(UrlUtils.cutScheme(connection.getHost())))
				.append(")")
				.toString();
		return ServerUtils.getServerName(baseName);
	}

	public static void updateServer(String serverName, String host, String connectionUrl, IService service, String sourcePath, String podPath, IProject deployProject, String routeURL, IServerWorkingCopy server) {
		String deployProjectName = ProjectUtils.getName(deployProject);
		updateServer(serverName, host, connectionUrl, deployProjectName, OpenShiftResourceUniqueId.get(service), sourcePath, podPath, routeURL, server);
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
	 * @deprecated no callers
	 */
	public static void updateServer(String serverName, String host, String connectionUrl, String deployProjectName, String serviceId, String sourcePath, String podPath, IServerWorkingCopy server) {
		updateServer(serverName, host, connectionUrl, deployProjectName, serviceId, sourcePath, podPath, null, server);
	}

	public static void updateServer(String serverName, String host, String connectionUrl, String deployProjectName, String serviceId, String sourcePath, String podPath, String routeURL, IServerWorkingCopy server) {
		updateServer(server);

		server.setName(serverName);
		server.setHost(host);

		server.setAttribute(ATTR_CONNECTIONURL, connectionUrl);
		server.setAttribute(ATTR_DEPLOYPROJECT, deployProjectName);
		server.setAttribute(ATTR_SOURCE_PATH, sourcePath);
		server.setAttribute(ATTR_POD_PATH, podPath);
		server.setAttribute(ATTR_SERVICE, serviceId);
		server.setAttribute(ATTR_ROUTE, routeURL);
	}

	/**
	 * Sets the given value for the given attribute in the given server and saves it.
	 * 
	 * @param attribute
	 * @param value
	 * @param server
	 * @throws CoreException
	 */
	public static void updateServer(String attribute, String value, IServerWorkingCopy server) throws CoreException {
		if (!StringUtils.isEmpty(attribute)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Could not update server project, setting name missing."));
		}
		if (!StringUtils.isEmpty(value)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not update server project, value for setting {0} is missing.", attribute)));
		}
		server.setAttribute(attribute, value);
		server.save(true, new NullProgressMonitor());
	}
	
	private static void updateServer(IServerWorkingCopy server) {
		server.setAttribute(IDeployableServer.SERVER_MODE, OpenShiftServer.OPENSHIFT3_MODE_ID);
		((ServerWorkingCopy) server).setAutoPublishSetting(Server.AUTO_PUBLISH_RESOURCE);
		server.setAttribute(IJBossToolingConstants.IGNORE_LAUNCH_COMMANDS, String.valueOf(Boolean.TRUE));
		int webPort = 80;//TODO should we determine the webPort from the route?
		server.setAttribute(IJBossToolingConstants.WEB_PORT, webPort);
		server.setAttribute(LIVERELOAD_PORT_KEY, webPort);//So that we can open via LiveReload
		server.setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, Boolean.FALSE.toString());
		server.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
		server.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
	}

	public static void updateServerProject(String connectionUrl, IService service, String sourcePath, String podPath, IProject project) {
		updateServerProject(connectionUrl, service, sourcePath, podPath, project);
	}

	public static void updateServerProject(String connectionUrl, IService service, String sourcePath, String podPath, String routeURL, IProject project) {
		updateServerProject(connectionUrl, OpenShiftResourceUniqueId.get(service), sourcePath, podPath, routeURL, project);
	}

	public static void updateServerProject(String connectionUrl, String serviceId, String sourcePath, String podPath, String routeURL, IProject project) {
		IEclipsePreferences node = ServerUtils.getProjectNode(SERVER_PROJECT_QUALIFIER, project);
		node.put(ATTR_CONNECTIONURL, connectionUrl);
		node.put(ATTR_DEPLOYPROJECT, project.getName());
		node.put(ATTR_SOURCE_PATH, sourcePath);
		node.put(ATTR_SERVICE, serviceId);
		updateProjectNode(ATTR_POD_PATH, podPath, node);
		updateProjectNode(ATTR_ROUTE, routeURL, node);

		saveProject(node);
	}

	private static void updateProjectNode(String attribute, String value, IEclipsePreferences node) {
		if (value != null) {
			node.put(attribute, value);
		} else {
			node.remove(attribute);
		}
	}
	
	public static void updateServerProject(String attribute, String value, IProject project) throws CoreException {
		if (!StringUtils.isEmpty(attribute)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Could not update server project, setting name missing."));
		}
		if (!StringUtils.isEmpty(value)) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not update server project, value for setting {0} is missing.", attribute)));
		}
		IEclipsePreferences node = ServerUtils.getProjectNode(SERVER_PROJECT_QUALIFIER, project);
		node.put(attribute, value);
		saveProject(node);
	}

	private static void saveProject(IEclipsePreferences node) {
		try {
			node.flush();
		} catch (BackingStoreException e) {
			// TODO: throw, dont swallow
			OpenShiftCoreActivator.pluginLog().logError(e);
		}
	}

	public static IModule findProjectModule(IProject p) {
		IModule[] all = org.eclipse.wst.server.core.ServerUtil.getModules(p);
		for( int i = 0; i < all.length; i++ ) {
			ModuleDelegate md = (ModuleDelegate)all[i].loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
			if( md instanceof ProjectModule 
					&& !(md instanceof org.eclipse.jst.j2ee.internal.deployables.BinaryFileModuleDelegate)) {
				return all[i];
			}
		}
		return null;
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

	public static IServerWorkingCopy create(String name) throws CoreException {
		final IServerWorkingCopy serverWorkingCopy = 
				(IServerWorkingCopy) getServerType().createServer(name, null, null);
		return serverWorkingCopy;
	}

	/**
	 * Returns 
	 * @param attributes
	 * @return
	 */
	public static Connection getConnection(IServerAttributes attributes) {
		try {
			String url = attributes.getAttribute(ATTR_CONNECTIONURL, (String) null);
			if( url == null)
					url = getProjectAttribute(ATTR_CONNECTIONURL, null, getDeployProject(attributes));
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
	
	/**
	 * Returns the service for the given server. It gets the service name from
	 * server settings and requests the service from the OpenShit server. It
	 * should thus never be called from the UI thread.
	 * 
	 * @param attributes the server (attributes) to get the service name from
	 * @param connection the connection (to the OpenShift server) to retrieve the service from
	 * @return the service 
	 */
	public static IService getService(IServerAttributes attributes, Connection connection) {
		// TODO: implement override project settings with server settings
		String uniqueId = attributes.getAttribute(ATTR_SERVICE, (String) null);
		if (uniqueId == null) {
			uniqueId = getProjectAttribute(ATTR_SERVICE, null, getDeployProject(attributes));
		}
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}
		String projectName = OpenShiftResourceUniqueId.getProjectName(uniqueId);
		List<IService> services = connection.getResources(ResourceKind.SERVICE, projectName);
		return OpenShiftResourceUniqueId.getByUniqueId(uniqueId, services);
	}

	public static String getRouteURL(IServerAttributes attributes) {
		String routeURL = attributes.getAttribute(ATTR_ROUTE, (String)null);
		if( routeURL == null ) 
			routeURL = getProjectAttribute(ATTR_ROUTE, null, getDeployProject(attributes));
		return routeURL;
	}

	public static String getPodPath(IServerAttributes attributes) {
		// TODO: implement override project settings with server settings
		String podPath = attributes.getAttribute(OpenShiftServerUtils.ATTR_POD_PATH, (String)null);
		if( podPath != null && !podPath.isEmpty()) {
			return podPath;
		}
		return getProjectAttribute(ATTR_POD_PATH, null, getDeployProject(attributes));
	}
	

	/**
	 * Creates an {@link RSync}
	 * @param server the {@link IServer} on which the {@code rsync} operation will be performed
	 * @return the {@link RSync} to be used to execute the command.
	 * @throws CoreException
	 */
	public static RSync createRSync(final IServer server) throws CoreException {
		final String location = OCBinary.getInstance().getLocation();
		if( location == null ) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					"Binary for oc-tools could not be found. Please open the OpenShift 3 Preference Page and set the location of the oc binary."));
		}
		
		final IService service = getService(server);
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the service to publish to.", server.getName())));
		}

		String podPath = getPodPath(server);
		if (StringUtils.isEmpty(podPath)) {
			podPath = loadPodPath(service, server);
			if (StringUtils.isEmpty(podPath)) {
				throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Server {0} could not determine the destination directory to publish to.", server.getName())));
			}
		}
		
		return new RSync(service, podPath, server);
	}

	public static String loadPodPath(IService service, IServer server) throws CoreException {
		return new PodDeploymentPathProvider().load(service, getConnection(server));
	}

	public static String getSourcePath(IServerAttributes attributes) {
		// TODO: implement override project settings with server settings
		String rawSourcePath = attributes.getAttribute(ATTR_SOURCE_PATH, (String)null);
		if( rawSourcePath == null || rawSourcePath.isEmpty()) {
			rawSourcePath = getProjectAttribute(ATTR_SOURCE_PATH, null, getDeployProject(attributes));
		}
		if (org.apache.commons.lang.StringUtils.isBlank(rawSourcePath)) {
			return rawSourcePath;
		}
		String path = VariablesHelper.replaceVariables(rawSourcePath);
		return path;
	}

	public static boolean isOverridesProject(IServerAttributes server) {
		return server.getAttribute(ATTR_OVERRIDE_PROJECT_SETTINGS, false);
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
	private static String getProjectAttribute(String name, String defaultValue, IProject project) {
		return ServerUtils.getProjectAttribute(name, defaultValue, SERVER_PROJECT_QUALIFIER, project);
	}
	
	/**
	 * Returns the deployment config for the given server (attributes). The
	 * match is done by the service that the given (openshift server) is bound
	 * to. This method does remote calls to the OpenShift server and thus should
	 * never be called from the UI thread.
	 * 
	 * @param attributes
	 * @return the deployment config for the given server
	 * 
	 * @see #getService(IServerAttributes)
	 * @see ResourceUtils#getPodsForService(IService, Collection)
	 */
	public static IDeploymentConfig getDeploymentConfig(IServerAttributes attributes) throws CoreException {
		Connection connection = getConnection(attributes);
		if (connection == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not find the connection for server {0}"
							+ "Your server adapter might refer to an inexistant connection."
							, attributes.getName())));
		}

		IService service = getService(attributes, connection);
		if (service == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not find the service for server {0}" 
							+ "Your server adapter might refer to an inexistant service.",
							attributes.getName())));
		}

		List<IPod> pods = connection.getResources(ResourceKind.POD, service.getProject().getName());
		List<IPod> servicePods = ResourceUtils.getPodsForService(service, pods);
		if (servicePods == null
				|| servicePods.isEmpty()) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not find pods for service {0} in connection {1}. "
							+ "OpenShift might be still building the pods for service {0}.", 
							service.getName(), connection.getHost())));
		}
		String dcName = ResourceUtils.getDeploymentConfigNameForPods(servicePods);
		if (dcName == null) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(
					NLS.bind("Could not find deployment config for {0}. "
							+ "Your build might be still running and pods not created yet or "
							+ "there might be no labels on your pods pointing to the wanted deployment config.", 
					attributes.getName())));
		}

		return connection.getResource(ResourceKind.DEPLOYMENT_CONFIG, service.getNamespace(), dcName);
	}
	
	public static boolean isEapStyle(IBuildConfig buildConfig) {
		if (buildConfig == null) {
			return false;
		}
		//First check buildconfig docker image name
		IBuildStrategy strategy = buildConfig.getBuildStrategy();
		DockerImageURI image = null;
		boolean isEapStyle = false;
		if (strategy instanceof ISourceBuildStrategy) {
			image = ((ISourceBuildStrategy) strategy).getImage();
		} else if (strategy instanceof ICustomBuildStrategy) {
			image = ((ICustomBuildStrategy) strategy).getImage();
		} else if (strategy instanceof IDockerBuildStrategy) {
			image = ((IDockerBuildStrategy) strategy).getBaseImage();
		} else if (strategy instanceof ISTIBuildStrategy) {
			image = ((ISTIBuildStrategy) strategy).getImage();
		}
		if (image != null) {
			isEapStyle = containsEapLikeKeywords(image.getName());
		}
		if (!isEapStyle) {
			//Check template labels as a last resort
			//not sure it's even possible to reach this point
			Map<String, String> labels = buildConfig.getLabels();
			if (labels != null) {
				String template = labels.get("template");
				isEapStyle = containsEapLikeKeywords(template);
			}
		}
		return isEapStyle;
	}
		
	public static boolean containsEapLikeKeywords(String label) {
		if (org.apache.commons.lang.StringUtils.isBlank(label)) {
			return false;
		}
		String lcLabel = label.toLowerCase();
		boolean isEapLike = EAP_LIKE_KEYWORDS.stream()
											 .filter(kw -> lcLabel.contains(kw))
											 .findFirst()
											 .isPresent();
		return isEapLike;
	}
	
}
