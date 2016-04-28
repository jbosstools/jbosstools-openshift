/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.utils.ObservableTreeItemUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardPageModel extends ServiceViewModel {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";
	public static final String PROPERTY_USE_INFERRED_POD_PATH = "useInferredPodPath";

	public static final String PROPERTY_SELECT_DEFAULT_ROUTE = "selectDefaultRoute";
	public static final String PROPERTY_ROUTE = "route";
	public static final String PROPERTY_ROUTES = "routes";
	public static final String PROPERTY_INVALID_OC_BINARY = "invalidOCBinary";

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
	private boolean invalidOCBinary = false;

	protected ServerSettingsWizardPageModel(IService service, IRoute route, org.eclipse.core.resources.IProject deployProject, 
			Connection connection, IServerWorkingCopy server) {
		this(service, route, deployProject, connection, server, false);
	}
	
	protected ServerSettingsWizardPageModel(IService service, IRoute route, org.eclipse.core.resources.IProject deployProject, 
			Connection connection, IServerWorkingCopy server, boolean invalidOCBinary) {
		super(service, connection);
		this.route = route;
		this.deployProject = deployProject;
		this.server = server;
		this.invalidOCBinary = invalidOCBinary;
	}

	protected void update(Connection connection, List<Connection> connections, 
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, boolean useInferredPodPath,
			IService service, List<ObservableTreeItem> serviceItems, 
			IRoute route, boolean isSelectDefaultRoute, Map<IProject, List<IRoute>> routesByProject,
			boolean invalidOCBinary) {
		update(connection, connections, service, serviceItems);
		updateProjects(projects);
		org.eclipse.core.resources.IProject oldDeployProject = this.deployProject;
		org.eclipse.core.resources.IProject newDeployProject = updateDeployProject(deployProject, projects, service);
		sourcePath = updateSourcePath(sourcePath, newDeployProject, oldDeployProject);
		List<IRoute> newRoutes = updateRoutes(service, routesByProject);
		updateRoute(route, newRoutes, service);
		updateSelectDefaultRoute(isSelectDefaultRoute);
		updateInvalidOCBinary(invalidOCBinary);
	}
	
	private void updateProjects(List<org.eclipse.core.resources.IProject> projects) {
		if(projects == this.projects) {
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

	protected org.eclipse.core.resources.IProject updateDeployProject(org.eclipse.core.resources.IProject newDeployProject, 
			List<org.eclipse.core.resources.IProject> projects, IService service) {
		if (newDeployProject == null
				|| !projects.contains(newDeployProject)) {
			newDeployProject = getDeployProject(service);
			if (newDeployProject == null) {
				newDeployProject =  getProjectOrDefault(newDeployProject, projects);
			}
		}
		firePropertyChange(PROPERTY_DEPLOYPROJECT, this.deployProject, this.deployProject = newDeployProject);
		return this.deployProject;
	}

	protected String updateSourcePath(String sourcePath, org.eclipse.core.resources.IProject newDeployProject, 
			org.eclipse.core.resources.IProject oldDeployProject) {
		if ((StringUtils.isEmpty(sourcePath)
				|| newDeployProject != oldDeployProject)
				&& ProjectUtils.isAccessible(newDeployProject)) {
			sourcePath = VariablesHelper.addWorkspacePrefix(newDeployProject.getFullPath().toString());
		}
		firePropertyChange(PROPERTY_SOURCE_PATH, this.sourcePath, this.sourcePath = sourcePath);
		return sourcePath;
	}

	/**
	 * Replaces choices in the route selector as needed.
	 * If choices are replaced calls updateRoute() to reset its selected value.
	 * @param routesByProject2 
	 * @param service 
	 * @return 
	 */
	protected List<IRoute> updateRoutes(IService service, Map<IProject, List<IRoute>> routesByProject) {
		this.routesByProject = routesByProject;

		List<IRoute> oldRoutes = new ArrayList<>(this.serviceRoutes);
		List<IRoute> routes = getAllRoutes(service);
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
	 * Updates the route that's selected. Chooses the route for the given service
	 * @param route
	 * @return 
	 * @return 
	 */
	protected IRoute updateRoute(IRoute route, List<IRoute> routes, IService service) {
		if (!isLoaded) {
			return null;
		}

		if (routes == null
				|| routes.isEmpty()) {
			route = null;
		} else {
			route = ResourceUtils.getRouteForService(service, routes);
			if(route == null 
					|| !routes.contains(route)) {
				route = routes.get(0);
			}
		}
		firePropertyChange(PROPERTY_ROUTE, this.route, this.route = route);
		return this.route;
	}

	private void updateSelectDefaultRoute(boolean selectDefaultRoute) {
		firePropertyChange(PROPERTY_SELECT_DEFAULT_ROUTE, this.selectDefaultRoute, this.selectDefaultRoute = selectDefaultRoute);
	}

	protected org.eclipse.core.resources.IProject getDeployProject(IService service) {
		if (service == null) {
			return null;
		}
		IProject openShiftProject = getOpenShiftProject(service);
		List<IBuildConfig> buildConfigs = getBuildConfigs(openShiftProject);
		IBuildConfig buildConfig = ResourceUtils.getBuildConfigForService(service, buildConfigs);
		return ResourceUtils.getWorkspaceProjectForBuildConfig(buildConfig, getProjects());
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), 
				project, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
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
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setPodPath(String podPath) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, podPath, this.useInferredPodPath,
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}

	public String getPodPath() {
		return podPath;
	}

	public boolean isUseInferredPodPath() {
		return this.useInferredPodPath;
	}

	public void setUseInferredPodPath(boolean useInferredPodPath) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, useInferredPodPath,
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}

	@Override
	public void setService(IService service) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				service, getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}
	
	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		if (project == null) {
			project = CollectionUtils.getFirstElement(projects);
		}
		return project;
	}

	@Override
	public void loadResources() {
		loadResources(getConnection());
	}

	@Override
	public void loadResources(Connection newConnection) {
		boolean serviceInitialized = this.service != null;
		this.isLoaded = false;

		super.loadResources(newConnection);
		List<IProject> openshiftProjects = ObservableTreeItemUtils.getAllModels(IProject.class, getServiceItems());
		setBuildConfigs(loadBuildConfigs(openshiftProjects, newConnection));
		setProjects(loadProjects());
		setRoutes(loadRoutes(getServiceItems()));

		this.isLoaded = true;

		if (serviceInitialized) {
			// initialized via a service/route (launched via openshift explorer)
			updateDeployProject(getDeployProject(service), projects, service);
		} else {
			// initialized via a project (launched via new server wizard)
			updateService(getService(deployProject, getServiceItems()), getServiceItems());
		}

		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				this.service, getServiceItems(),
				this.route, selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}

	private List<IBuildConfig> getBuildConfigs(IProject project) {
		if (buildConfigsByProject == null) {
			return null;
		}
		return buildConfigsByProject.get(project);
	}

	protected IService getService(org.eclipse.core.resources.IProject project, List<ObservableTreeItem> services) {
		List<IService> allServices = ObservableTreeItemUtils.getAllModels(IService.class, 
				services.stream().flatMap(ObservableTreeItemUtils::flatten).collect(Collectors.toList()));
		return allServices.stream()
			.filter(service -> ObjectUtils.equals(project, getDeployProject(service)))
			.findFirst().orElse(null);
	}
	
	private Map<IProject, List<IBuildConfig>> loadBuildConfigs(List<IProject> projects, Connection connection) {
		if (projects == null
				|| projects.isEmpty()) {
			return Collections.emptyMap();
		}
		return projects.stream()
				.collect(Collectors.toMap(
						project -> project, 
						project -> {
							List<IBuildConfig> buildConfigs = connection.getResources(ResourceKind.BUILD_CONFIG, project.getName());
							return buildConfigs;
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
		String serverName = OpenShiftServerUtils.getServerName(getService(), getConnection());
		String host = getHost(getRoute());
		String routeURL = getRouteURL(isSelectDefaultRoute(), getRoute());
		OpenShiftServerUtils.updateServer(
				serverName, host, connectionUrl, getService(), sourcePath, podPath, deployProject, routeURL, server);
		server.setAttribute(OpenShiftServerUtils.SERVER_START_ON_CREATION, true);
		
		// Set the profile
		String profile = getProfileId();
		ServerProfileModel.setProfile(server, profile);
		
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, getService(), sourcePath, podPath, routeURL, deployProject);

		IModule[] matchingModules = ServerUtil.getModules(deployProject);
		if( matchingModules != null && matchingModules.length > 0) {
			try {
				server.modifyModules(matchingModules, new IModule[]{}, new NullProgressMonitor());
			} catch(CoreException ce) {
				throw new OpenShiftException(ce, "Could not get add modules to server ", server.getName());
			}
		}
	}
	
	private String getProfileId() {
		// currently supported profiles are openshift3 or openshift3.eap
		// we need to determine if the current service represents an app that requires
		// eap-style behavior or normal behavior
		boolean isEapProfile = false;
		IService service = getService();
		if (service != null) {
			IProject project = getOpenShiftProject(service);
			if (project != null) {
				List<IBuildConfig> buildConfigs = getBuildConfigs(project);
				if (buildConfigs != null) {
					IBuildConfig buildConfig = ResourceUtils.getBuildConfigForService(service, buildConfigs);
					isEapProfile = OpenShiftServerUtils.isEapStyle(buildConfig);
				}
			}
		}

		if(isEapProfile) {
			return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3_EAP;
		}
		return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3;
	}

	private String getHost(IRoute route) {
		if (route == null) {
			return "";
		}
		return UrlUtils.getHost(route.getURL());
	}

	private String getRouteURL(boolean isDefaultRoute, IRoute route) {
		if (!isDefaultRoute || route == null) {
			return null;
		}
		return route.getURL();
	}
	
	public boolean isSelectDefaultRoute() {
		return selectDefaultRoute;
	}

	public void setSelectDefaultRoute(boolean selectDefaultRoute) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(),
				this.route, selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
	}

	protected void setRoutes(Map<IProject, List<IRoute>> routesByProject) {
		updateRoutes(service, routesByProject);
	}

	public List<IRoute> getRoutes() {
		if (getService() == null) {
			return Collections.emptyList();
		}
		return getAllRoutes(getService());
	}

	public IRoute getRoute() {
		if (!isLoaded) {
			return null;
		}
		// reveal selected route only once model is loaded
		return route;
	}

	public void setRoute(IRoute newRoute) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(),
				route, this.selectDefaultRoute, this.routesByProject,
				this.invalidOCBinary);
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

	protected List<IRoute> getAllRoutes(IService service) {
		IProject project = getOpenShiftProject(service);
		if (project == null) {
			return Collections.emptyList();
		}
		return getAllRoutes(project);
	}

	protected List<IRoute> getAllRoutes(IProject project) {
		return routesByProject.get(project);
	}

	protected IProject getOpenShiftProject(IRoute route) {
		return routesByProject.keySet().stream()
					.filter(project -> ((List<IRoute>)routesByProject.get(project)).contains(route))
					.findFirst().orElseGet(null);
	}

	public IServer saveServer(IProgressMonitor monitor) throws CoreException {
		return server.save(true, monitor);
	}
	
	private String getConnectionUrl(Connection connection) {
		ConnectionURL connectionUrl;
		try {
			connectionUrl = ConnectionURL.forConnection(connection);
			return connectionUrl.toString();
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftException(e, "Could not get url for connection {0}", connection.getHost());
		}
	}

    private void updateInvalidOCBinary(boolean invalidOCBinary) {
        firePropertyChange(PROPERTY_INVALID_OC_BINARY, this.invalidOCBinary,
                this.invalidOCBinary = invalidOCBinary);
    }

    public boolean isInvalidOCBinary() {
        return invalidOCBinary;
    }

    public void setInvalidOCBinary(boolean invalidOCBinary) {
        update(getConnection(), getConnections(), 
                this.deployProject, this.projects, 
                this.sourcePath, this.podPath, this.useInferredPodPath,
                getService(), getServiceItems(),
                route, this.selectDefaultRoute, this.routesByProject,
                invalidOCBinary);
    }
}
