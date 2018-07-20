/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ExtensionUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryValidator;
import org.jboss.tools.openshift.internal.core.util.RSyncValidator.RsyncStatus;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.utils.ObservableTreeItemUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 * @author Jeff Maury
 */
public class ServerSettingsWizardPageModel extends ServerResourceViewModel implements IResourceChangeListener {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";
	public static final String PROPERTY_USE_INFERRED_POD_PATH = "useInferredPodPath";

	public static final String PROPERTY_SELECT_DEFAULT_ROUTE = "selectDefaultRoute";
	public static final String PROPERTY_ROUTE = "route";
	public static final String PROPERTY_ROUTES = "routes";
	public static final String PROPERTY_USE_IMAGE_DEVMODE_KEY = "useImageDevmodeKey";
	public static final String PROPERTY_DEVMODE_KEY = "devmodeKey";
	public static final String PROPERTY_USE_IMAGE_DEBUG_PORT_KEY = "useImageDebugPortKey";
	public static final String PROPERTY_DEBUG_PORT_KEY = "debugPortKey";
	public static final String PROPERTY_USE_IMAGE_DEBUG_PORT_VALUE = "useImageDebugPortValue";
	public static final String PROPERTY_DEBUG_PORT_VALUE = "debugPortValue";
	public static final String PROPERTY_OC_BINARY_STATUS = "OCBinaryStatus";
	public static final String PROPERTY_RSYNC_STATUS = "rsyncStatus";

	private static final String PROFILE_DETECTOR_EP_ID = "org.jboss.tools.openshift.core.serverAdapterProfileDetector";

	protected org.eclipse.core.resources.IProject deployProject;
	protected List<org.eclipse.core.resources.IProject> projects = new ArrayList<>();
	private String sourcePath;
	protected String podPath;
	protected String inferredPodPath = null;
	private IServerWorkingCopy server;
	private boolean selectDefaultRoute = false;
	private IRoute route;
	private Map<IProject, List<IRoute>> routesByProject = new HashMap<>();
	private List<IRoute> serviceRoutes = new ArrayList<>();
	private boolean isLoaded = false;
	private Map<IProject, List<IBuildConfig>> buildConfigsByProject;
	private boolean useInferredPodPath = true;
	private IStatus ocBinaryStatus;
	protected boolean useImageDevmodeKey = true;
	protected String devmodeKey;
	protected boolean useImageDebugPortKey = true;
	protected String debugPortKey;
	protected boolean useImageDebugPortValue = true;
	protected String debugPortValue;
	protected RsyncStatus rsyncStatus;

	protected ServerSettingsWizardPageModel(IResource resource, IRoute route,
			org.eclipse.core.resources.IProject deployProject, Connection connection, IServerWorkingCopy server) {
		this(resource, route, deployProject, connection, server, ValidationStatus.cancel("oc binary not verified"), RsyncStatus.OK);
	}

	public ServerSettingsWizardPageModel(IResource resource, IRoute route,
			org.eclipse.core.resources.IProject deployProject, Connection connection, IServerWorkingCopy server,
			IStatus ocBinaryStatus, RsyncStatus rsyncStatus) {
		super(resource, connection);
		this.route = route;
		this.deployProject = deployProject;
		this.server = server;
		init(server);
		this.ocBinaryStatus = ocBinaryStatus;
		this.rsyncStatus = rsyncStatus;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	private void init(IServerWorkingCopy server) {
		this.devmodeKey = OpenShiftServerUtils.getDevmodeKey(server);
		this.useImageDevmodeKey = isUseImageDevmodeKey(devmodeKey);
		this.debugPortKey = OpenShiftServerUtils.getDebugPortKey(server);
		this.useImageDebugPortKey = isUseImageDebugPortKey(debugPortKey);
		this.debugPortValue = OpenShiftServerUtils.getDebugPort(server);
		this.useImageDebugPortValue = isUseImageDebugPortValue(debugPortValue);
	}

	protected void update(Connection connection, List<Connection> connections,
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects,
			String sourcePath, String podPath, boolean useInferredPodPath, IResource resource,
			List<ObservableTreeItem> resourceItems, IRoute route, boolean isSelectDefaultRoute,
			Map<IProject, List<IRoute>> routesByProject, IStatus ocBinaryStatus, boolean useImageDevmodeKey,
			String devmodeKey, boolean useImageDebugPortKey, String debugPortKey, boolean useImageDebugPortValue,
			String debugPortValue, RsyncStatus rsyncStatus) {
		update(connection, connections, resource, resourceItems);
		updateProjects(projects);
		org.eclipse.core.resources.IProject oldDeployProject = this.deployProject;
		org.eclipse.core.resources.IProject newDeployProject = updateDeployProject(deployProject, projects, resource);
		sourcePath = updateSourcePath(sourcePath, newDeployProject, oldDeployProject);
		List<IRoute> newRoutes = updateRoutes(resource, routesByProject);
		updateRoute(route, newRoutes, resource);
		updateSelectDefaultRoute(isSelectDefaultRoute);
		updateOCBinaryStatus(ocBinaryStatus);
		firePropertyChange(PROPERTY_USE_INFERRED_POD_PATH, this.useInferredPodPath, this.useInferredPodPath = useInferredPodPath);
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = podPath);
		updateDevmode(useImageDevmodeKey, devmodeKey);
		updateDebugPort(useImageDebugPortKey, debugPortKey, useImageDebugPortValue, debugPortValue);
		updateRsyncStatus(rsyncStatus);
	}

	private void updateProjects(List<org.eclipse.core.resources.IProject> projects) {
		if (projects == this.projects) {
			return; // happens when other properties are changed, avoid unnecessary work
		}
		List<org.eclipse.core.resources.IProject> oldProjects = new ArrayList<>(this.projects);
		// ensure we're not operating on the same list
		List<org.eclipse.core.resources.IProject> newProjects = new ArrayList<>();
		if (projects != null) {
			newProjects.addAll(projects);
		}
		this.projects.clear();
		this.projects.addAll(newProjects);
		firePropertyChange(PROPERTY_PROJECTS, oldProjects, this.projects);
	}

	protected org.eclipse.core.resources.IProject updateDeployProject(
			org.eclipse.core.resources.IProject newDeployProject, List<org.eclipse.core.resources.IProject> projects,
			IResource resource) {
		if (newDeployProject == null 
				|| !projects.contains(newDeployProject)) {
			newDeployProject = getDeployProject(resource);
			if (newDeployProject == null) {
				newDeployProject = getProjectOrDefault(newDeployProject, projects);
			}
		}
		firePropertyChange(PROPERTY_DEPLOYPROJECT, this.deployProject, this.deployProject = newDeployProject);
		return this.deployProject;
	}

	protected String updateSourcePath(String sourcePath, org.eclipse.core.resources.IProject newDeployProject,
			org.eclipse.core.resources.IProject oldDeployProject) {
		if ((StringUtils.isEmpty(sourcePath) || newDeployProject != oldDeployProject)
				&& ProjectUtils.isAccessible(newDeployProject)) {
			sourcePath = VariablesHelper.addWorkspacePrefix(newDeployProject.getFullPath().toString());
		}
		firePropertyChange(PROPERTY_SOURCE_PATH, this.sourcePath, this.sourcePath = sourcePath);
		return sourcePath;
	}

	/**
	 * Replaces choices in the route selector as needed. If choices are replaced
	 * calls updateRoute() to reset its selected value.
	 * 
	 * @param routesByProject2
	 * @param resource
	 * @return
	 */
	protected List<IRoute> updateRoutes(IResource resource, Map<IProject, List<IRoute>> routesByProject) {
		this.routesByProject = routesByProject;

		List<IRoute> oldRoutes = new ArrayList<>(this.serviceRoutes);
		List<IRoute> routes = getRoutes(resource);
		this.serviceRoutes.clear();
		this.serviceRoutes.addAll(routes);
		resetSelectedRoute();
		firePropertyChange(PROPERTY_ROUTES, oldRoutes, this.serviceRoutes);

		return routes;
	}

	// workaround needed for a possible (?) bug in databinding
	private void resetSelectedRoute() {
		if (this.route != null) {
			firePropertyChange(PROPERTY_ROUTE, this.route, this.route = null);
		}
	}

	/**
	 * Updates the route that's selected. Chooses the route for the given resource
	 * 
	 * @param route
	 * @return
	 * @return
	 */
	protected IRoute updateRoute(IRoute route, List<IRoute> routes, IResource resource) {
		if (!isLoaded) {
			return null;
		}

		if (routes == null || routes.isEmpty()) {
			route = null;
		} else if (route == null || !routes.contains(route)) {
			if (resource instanceof IService) {
				route = ResourceUtils.getRouteFor((IService) resource, routes);
			}
			if (route == null || !routes.contains(route)) {
				route = routes.get(0);
			}
		}
		firePropertyChange(PROPERTY_ROUTE, this.route, this.route = route);
		return this.route;
	}

	private void updateSelectDefaultRoute(boolean selectDefaultRoute) {
		firePropertyChange(PROPERTY_SELECT_DEFAULT_ROUTE, this.selectDefaultRoute,
				this.selectDefaultRoute = selectDefaultRoute);
	}

	protected org.eclipse.core.resources.IProject getDeployProject(IResource resource) {
		// TODO: JBIDE-23490 check if association can be done for non service resources
		if (resource == null) {
			return null;
		}
		IProject openShiftProject = getOpenShiftProject(resource);
		List<IBuildConfig> buildConfigs = getBuildConfigs(openShiftProject);
		IBuildConfig buildConfig = ResourceUtils.getBuildConfigFor(resource, buildConfigs);
		return ResourceUtils.getWorkspaceProjectFor(buildConfig, getProjects());
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), project, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public org.eclipse.core.resources.IProject getDeployProject() {
		return deployProject;
	}

	protected void setProjects(List<org.eclipse.core.resources.IProject> projects) {
		updateProjects(projects);
	}

	public List<org.eclipse.core.resources.IProject> getProjects() {
		return projects;
	}

	public void setSourcePath(String sourcePath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setPodPath(String podPath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public String getPodPath() {
		return podPath;
	}

	public boolean isUseInferredPodPath() {
		return this.useInferredPodPath;
	}

	public void setUseInferredPodPath(boolean useInferredPodPath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	@Override
	public void setResource(IResource resource) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, resource, getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project,
			List<org.eclipse.core.resources.IProject> projects) {
		if (project == null) {
			project = projects.stream().findFirst().orElse(null);
		}
		return project;
	}

	@Override
	public void loadResources() {
		loadResources(getConnection());
	}

	@Override
	public void loadResources(Connection newConnection) {
		if (newConnection == null) {
			return;
		}

		boolean serviceInitialized = this.resource != null;
		this.isLoaded = false;

		setProjects(loadProjects());
		super.loadResources(newConnection);
		List<IProject> openshiftProjects = ObservableTreeItemUtils.getAllModels(IProject.class, getResourceItems());
		setBuildConfigs(loadBuildConfigs(openshiftProjects, newConnection));
		setProjects(loadProjects());
		setRoutes(loadRoutes(getResourceItems()));
		setOCBinaryStatus(validateOCBinary(newConnection, new NullProgressMonitor()));

		this.isLoaded = true;

		if (serviceInitialized) {
			// initialized via a resource/route (launched via openshift explorer)
			updateDeployProject(getDeployProject(resource), projects, resource);
		} else {
			// initialized via a project (launched via new server wizard)
			updateResource(getService(deployProject, getResourceItems()), getResourceItems());
		}

		update(newConnection, getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, this.resource, getResourceItems(), this.route, selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	private List<IBuildConfig> getBuildConfigs(IProject project) {
		if (buildConfigsByProject == null) {
			return null;
		}
		return buildConfigsByProject.get(project);
	}

	protected IService getService(org.eclipse.core.resources.IProject project, List<ObservableTreeItem> services) {
		if (!ProjectUtils.isAccessible(project) || CollectionUtils.isEmpty(services)) {
			return null;
		}
		// JBIDE-23490: check default resource
		List<IService> allServices = ObservableTreeItemUtils.getAllModels(IService.class,
				services.stream().flatMap(ObservableTreeItemUtils::flatten).collect(Collectors.toList()));
		return allServices.stream().filter(service -> ObjectUtils.equals(project, getDeployProject(service)))
				.findFirst().orElse(null);
	}

	private Map<IProject, List<IBuildConfig>> loadBuildConfigs(List<IProject> projects, Connection connection) {
		if (projects == null || projects.isEmpty()) {
			return Collections.emptyMap();
		}

		return projects.stream().collect(Collectors.toMap(project -> project, project -> {
			return connection.getResources(ResourceKind.BUILD_CONFIG, project.getName());
		}));
	}

	private void setBuildConfigs(Map<IProject, List<IBuildConfig>> buildConfigsByProject) {
		this.buildConfigsByProject = buildConfigsByProject;
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
	}

	protected Map<IProject, List<IRoute>> loadRoutes(List<ObservableTreeItem> serviceItems) {
		List<IProject> projects = ObservableTreeItemUtils.getAllModels(IProject.class, serviceItems);
		return projects.stream()
				.collect(Collectors.toMap(project -> project, project -> project.getResources(ResourceKind.ROUTE)));
	}

	public void updateServer() throws OpenShiftException {
		updateServer(server);
	}

	private void updateServer(IServerWorkingCopy server) throws OpenShiftException {
		String connectionUrl = getConnectionUrl(getConnection());
		String serverName = OpenShiftServerUtils.getServerName(getResource(), getConnection());
		String host = getHost(getRoute(), getConnection());
		String routeURL = getRouteURL(isSelectDefaultRoute(), getRoute());
		String podPath = useInferredPodPath ? "" : this.podPath;
		String devmodeKey = getDevmodeKey(useImageDevmodeKey, this.devmodeKey);
		String debugPortKey = getDebugPortKey(useImageDebugPortKey, this.debugPortKey);
		String debugPortValue = getDebugPortValue(useImageDebugPortValue, this.debugPortValue);
		String profileId = getProfileId();

		OpenShiftServerUtils.updateServer(serverName, host, connectionUrl, getResource(), sourcePath, podPath,
				deployProject, routeURL, devmodeKey, debugPortKey, debugPortValue, profileId, server);

		updateServerProject(connectionUrl, getResource(), sourcePath, routeURL, podPath, devmodeKey, debugPortKey,
				debugPortValue, deployProject);

		updateModules(server);
	}

	// for testing purposes
	protected void updateServerProject(String connectionUrl, IResource resource, String sourcePath, String routeURL,
			String podPath, String devmodeKey, String debugPortKey, String debugPortValue,
			org.eclipse.core.resources.IProject deployProject) {
		OpenShiftServerUtils.updateServerProject(connectionUrl, resource, sourcePath, podPath, routeURL,
				devmodeKey, debugPortKey, debugPortValue, deployProject);
	}

	protected void updateModules(IServerWorkingCopy server) {
		IModule[] matchingModules = ServerUtil.getModules(deployProject);
		if (matchingModules != null && matchingModules.length > 0) {
			try {
				server.modifyModules(matchingModules, new IModule[] {}, new NullProgressMonitor());
			} catch (CoreException ce) {
				throw new OpenShiftException(ce, "Could not get add modules to server ", server.getName());
			}
		}
	}

	protected String getProfileId() {
		Collection<IConfigurationElement> configurationElements = ExtensionUtils
				.getExtensionConfigurations(PROFILE_DETECTOR_EP_ID);
		for (IConfigurationElement configurationElement : configurationElements) {
			try {
				Object extension = configurationElement.createExecutableExtension("class");
				if (extension instanceof IOpenshiftServerAdapterProfileDetector) {
					IOpenshiftServerAdapterProfileDetector detector = (IOpenshiftServerAdapterProfileDetector) extension;
					if (detector.detect(getConnection(), getResource(), getDeployProject())) {
						return detector.getProfile();
					}
				}
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3;
	}

	protected String getHost(IRoute route) {
		return getHost(route, null);
	}

	protected String getHost(IRoute route, IConnection connection) {
		if (route != null) {
			return UrlUtils.getHost(route.getURL());
		} else if (connection != null) {
			return UrlUtils.getHost(connection.getHost());
		}
		return "";
	}

	protected String getRouteURL(boolean isSelectDefaultRoute, IRoute route) {
		if (!isSelectDefaultRoute || route == null) {
			return null;
		}
		return route.getURL();
	}

	public boolean isSelectDefaultRoute() {
		return selectDefaultRoute;
	}

	public void setSelectDefaultRoute(boolean selectDefaultRoute) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	protected void setRoutes(Map<IProject, List<IRoute>> routesByProject) {
		updateRoutes(resource, routesByProject);
	}

	/**
	 * Returns the routes that match the resource that's currently selected.
	 * 
	 * @return the routes that match the selected resource
	 * 
	 * @see #getResource()
	 * @see #getAllRoutes(IService)
	 */
	public List<IRoute> getRoutes() {
		if (getResource() == null) {
			return Collections.emptyList();
		}
		return getRoutes(getResource());
	}

	protected List<IRoute> getRoutes(IResource resource) {
		if (resource instanceof IService) {
			return ResourceUtils.getRoutesFor((IService) resource, getAllRoutes(resource));
		} else {
			return Collections.emptyList();
		}
	}

	public IRoute getRoute() {
		if (!isLoaded) {
			return null;
		}
		// reveal selected route only once model is loaded
		return route;
	}

	public void setRoute(IRoute newRoute) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), newRoute, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	protected List<IRoute> getAllRoutes(IRoute route) {
		IProject project = getOpenShiftProject(route);
		if (project == null) {
			return Collections.emptyList();
		}
		return getAllRoutes(project);
	}

	protected Map<IProject, List<IRoute>> getAllRoutes() {
		return routesByProject;
	}

	protected List<IRoute> getAllRoutes(IResource resource) {
		IProject project = resource.getProject();
		if (project == null) {
			return Collections.emptyList();
		}
		return getAllRoutes(project);
	}

	protected List<IRoute> getAllRoutes(IProject project) {
		return routesByProject.get(project);
	}

	protected IProject getOpenShiftProject(IRoute route) {
		return route.getProject();
	}

	public IServer saveServer(IProgressMonitor monitor) throws CoreException {
		return server.save(true, monitor);
	}

	protected String getConnectionUrl(IConnection connection) {
		if (connection == null) {
			return null;
		}
		try {
			ConnectionURL connectionUrl = ConnectionURL.forConnection(connection);
			return connectionUrl.toString();
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftException(e, "Could not get url for connection {0}", connection.getHost());
		}
	}

	private void updateOCBinaryStatus(IStatus ocBinaryStatus) {
		firePropertyChange(PROPERTY_OC_BINARY_STATUS, this.ocBinaryStatus, this.ocBinaryStatus = ocBinaryStatus);
	}

	public IStatus getOCBinaryStatus() {
		return ocBinaryStatus;
	}

	protected void setOCBinaryStatus(IStatus ocBinaryStatus) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public void validateOCBinary(IProgressMonitor monitor) {
		IStatus status = validateOCBinary(getConnection(), monitor);
		setOCBinaryStatus(status);
	}

	protected IStatus validateOCBinary(Connection connection, IProgressMonitor monitor) {
		IStatus status = new OCBinaryValidator(
				OCBinary.getInstance().getPath(connection)).getStatus(monitor);
		// turn non-ok status into errors 
		if (status.isOK()) {
			return status;
		} else {
			return StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, status.getMessage());
		}
	}

	private void updateRsyncStatus(RsyncStatus rsyncStatus) {
		firePropertyChange(PROPERTY_RSYNC_STATUS, this.rsyncStatus, this.rsyncStatus = rsyncStatus);
	}

	public RsyncStatus getRsyncStatus() {
		return rsyncStatus;
	}

	public void setRsyncStatus(RsyncStatus rsyncStatus) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				rsyncStatus);
	}

	private void updateDevmode(boolean useImageDevmodeKey, String devmodeKey) {
		String oldDevmodeKey = this.devmodeKey;
		this.devmodeKey = getDevmodeKey(useImageDevmodeKey, devmodeKey);
		firePropertyChange(PROPERTY_DEVMODE_KEY, oldDevmodeKey, this.devmodeKey);

		firePropertyChange(PROPERTY_USE_IMAGE_DEVMODE_KEY, this.useImageDevmodeKey,
				this.useImageDevmodeKey = useImageDevmodeKey);
	}

	public void setUseImageDevmodeKey(boolean useImageDevmodeKey) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public boolean isUseImageDevmodeKey() {
		return useImageDevmodeKey;
	}

	protected boolean isUseImageDevmodeKey(String devmodeKey) {
		return StringUtils.isEmpty(devmodeKey);
	}

	public void setDevmodeKey(String devmodeKey) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public String getDevmodeKey() {
		return devmodeKey;
	}

	private String getDevmodeKey(boolean useImageDevmodeKey, String devmodeKey) {
		if (useImageDevmodeKey) {
			devmodeKey = null;
		}
		return devmodeKey;
	}

	public void setUseImageDebugPortKey(boolean useImageDebugPortKey) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public boolean isUseImageDebugPortKey() {
		return useImageDebugPortKey;
	}

	protected boolean isUseImageDebugPortKey(String debugPortKey) {
		return StringUtils.isEmpty(debugPortKey);
	}

	public void setDebugPortKey(String debugPortKey) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, debugPortKey, this.useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public String getDebugPortKey() {
		return debugPortKey;
	}

	private String getDebugPortKey(boolean useImageDebugPortKey, String debugPortKey) {
		if (useImageDebugPortKey) {
			return null;
		} else {
			return debugPortKey;
		}
	}

	public void setUseImageDebugPortValue(boolean useImageDebugPortValue) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, useImageDebugPortValue, this.debugPortValue,
				this.rsyncStatus);
	}

	public boolean isUseImageDebugPortValue() {
		return useImageDebugPortValue;
	}

	protected boolean isUseImageDebugPortValue(String debugPortValue) {
		return StringUtils.isEmpty(debugPortValue);
	}

	public void setDebugPortValue(String debugPortValue) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, this.podPath,
				this.useInferredPodPath, getResource(), getResourceItems(), this.route, this.selectDefaultRoute,
				this.routesByProject, this.ocBinaryStatus, this.useImageDevmodeKey, this.devmodeKey,
				this.useImageDebugPortKey, this.debugPortKey, this.useImageDebugPortValue, debugPortValue,
				this.rsyncStatus);
	}

	public String getDebugPortValue() {
		return debugPortValue;
	}

	private String getDebugPortValue(boolean useImageDebugPortValue, String debugPortValue) {
		if (useImageDebugPortValue) {
			return null;
		} else {
			return debugPortValue;
		}
	}

	private void updateDebugPort(boolean useImageDebugPortKey, String debugPortKey, boolean useImageDebugPortValue,
			String debugPortValue) {
		String oldDebugPortKey = this.debugPortKey;
		this.debugPortKey = getDebugPortKey(useImageDebugPortKey, debugPortKey);
		String oldDebugPort = this.debugPortValue;
		this.debugPortValue = getDebugPortValue(useImageDebugPortValue, debugPortValue);
		firePropertyChange(PROPERTY_DEBUG_PORT_KEY, oldDebugPortKey, this.debugPortKey);
		firePropertyChange(PROPERTY_DEBUG_PORT_VALUE, oldDebugPort, this.debugPortValue);

		firePropertyChange(PROPERTY_USE_IMAGE_DEBUG_PORT_KEY, this.useImageDebugPortKey,
				this.useImageDebugPortKey = useImageDebugPortKey);
		firePropertyChange(PROPERTY_USE_IMAGE_DEBUG_PORT_VALUE, this.useImageDebugPortValue,
				this.useImageDebugPortValue = useImageDebugPortValue);
	}

	protected IServerWorkingCopy getServer() {
		return server;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		boolean[] needReload = new boolean[1];
		try {
			event.getDelta().accept((delta) -> {
				if ((delta.getResource().getType() == org.eclipse.core.resources.IResource.PROJECT)
						&& ((delta.getKind() == IResourceDelta.ADDED) || (delta.getKind() == IResourceDelta.REMOVED))) {
					needReload[0] = true;
				}
				return true;
			}, true);
			if (needReload[0]) {
				setProjects(loadProjects());
			}
		} catch (CoreException e) {
			OpenShiftUIActivator.log(Status.ERROR, e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
