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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
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
 * @author Jeff Maury
 */
public class ServerSettingsWizardPageModel extends ServiceViewModel implements IResourceChangeListener {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";
	public static final String PROPERTY_USE_INFERRED_POD_PATH = "useInferredPodPath";

	public static final String PROPERTY_SELECT_DEFAULT_ROUTE = "selectDefaultRoute";
	public static final String PROPERTY_ROUTE = "route";
	public static final String PROPERTY_ROUTES = "routes";
	public static final String PROPERTY_OC_BINARY_STATUS = "OCBinaryStatus";

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
	private IStatus ocBinaryStatus = Status.OK_STATUS;

	protected ServerSettingsWizardPageModel(IService service, IRoute route, org.eclipse.core.resources.IProject deployProject, 
			Connection connection, IServerWorkingCopy server) {
		this(service, route, deployProject, connection, server, Status.OK_STATUS);
	}

	protected ServerSettingsWizardPageModel(IService service, IRoute route, org.eclipse.core.resources.IProject deployProject, 
			Connection connection, IServerWorkingCopy server, IStatus ocBinaryStatus) {
		super(service, connection);
		this.route = route;
		this.deployProject = deployProject;
		this.server = server;
		this.ocBinaryStatus = ocBinaryStatus;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	protected void update(IConnection connection, List<IConnection> connections, 
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, boolean useInferredPodPath,
			IService service, List<ObservableTreeItem> serviceItems, 
			IRoute route, boolean isSelectDefaultRoute, Map<IProject, List<IRoute>> routesByProject,
			IStatus ocBinaryStatus) {
		update(connection, connections, service, serviceItems);
		updateProjects(projects);
		org.eclipse.core.resources.IProject oldDeployProject = this.deployProject;
		org.eclipse.core.resources.IProject newDeployProject = updateDeployProject(deployProject, projects, service);
		sourcePath = updateSourcePath(sourcePath, newDeployProject, oldDeployProject);
		List<IRoute> newRoutes = updateRoutes(service, routesByProject);
		updateRoute(route, newRoutes, service);
		updateSelectDefaultRoute(isSelectDefaultRoute);
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath= podPath);
		updateOCBinaryStatus(ocBinaryStatus);
		firePropertyChange(PROPERTY_USE_INFERRED_POD_PATH, this.useInferredPodPath, this.useInferredPodPath = useInferredPodPath);
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = podPath);
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
		List<IRoute> routes = getRoutes(service);
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
		} else if(route == null || !routes.contains(route)) {
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
		IBuildConfig buildConfig = ResourceUtils.getBuildConfigFor(service, buildConfigs);
		return ResourceUtils.getWorkspaceProjectForBuildConfig(buildConfig, getProjects());
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), 
				project, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.ocBinaryStatus);
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
				this.ocBinaryStatus);
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
				this.ocBinaryStatus);
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
				this.ocBinaryStatus);
	}

	@Override
	public void setService(IService service) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				service, getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject,
				this.ocBinaryStatus);
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
	public void loadResources(IConnection newConnection) {
		boolean serviceInitialized = this.service != null;
		this.isLoaded = false;
		setProjects(loadProjects());
		super.loadResources(newConnection);
		List<IProject> openshiftProjects = ObservableTreeItemUtils.getAllModels(IProject.class, getServiceItems());
		setBuildConfigs(loadBuildConfigs(openshiftProjects, newConnection));
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
				this.ocBinaryStatus);
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
	
	private Map<IProject, List<IBuildConfig>> loadBuildConfigs(List<IProject> projects, IConnection connection) {
		if (projects == null
				|| projects.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Connection c2 = (Connection)connection;
		return projects.stream()
				.collect(Collectors.toMap(
						project -> project, 
						project -> {
							List<IBuildConfig> buildConfigs = c2.getResources(ResourceKind.BUILD_CONFIG, project.getName());
							return buildConfigs;
						}));
	}

	private void setBuildConfigs(Map<IProject, List<IBuildConfig>> buildConfigsByProject) {
		this.buildConfigsByProject = buildConfigsByProject;
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		List<org.eclipse.core.resources.IProject> p = ProjectUtils.getAllAccessibleProjects();
		return p;
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
		String podPath = useInferredPodPath ? "": this.podPath;
		OpenShiftServerUtils.updateServer(
				serverName, host, connectionUrl, getService(), sourcePath, podPath, deployProject, routeURL, server);
		server.setAttribute(OpenShiftServerUtils.SERVER_START_ON_CREATION, true);
		
		// Set the profile
		String profile = getProfileId();
		ServerProfileModel.setProfile(server, profile);
		
		updateServerProject(connectionUrl, getService(), sourcePath, podPath, routeURL, deployProject);

		IModule[] matchingModules = ServerUtil.getModules(deployProject);
		if( matchingModules != null && matchingModules.length > 0) {
			try {
				server.modifyModules(matchingModules, new IModule[]{}, new NullProgressMonitor());
			} catch(CoreException ce) {
				throw new OpenShiftException(ce, "Could not get add modules to server ", server.getName());
			}
		}
	}

	protected void updateServerProject(String connectionUrl, IService service, String sourcePath, String podPath, 
			String routeURL, org.eclipse.core.resources.IProject deployProject) {
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, service, sourcePath, podPath, routeURL, deployProject);
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
					IBuildConfig buildConfig = ResourceUtils.getBuildConfigFor(service, buildConfigs);
					isEapProfile = OpenShiftServerUtils.isEapStyle(buildConfig);
				}
			}
		}

		if(isEapProfile) {
			return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3_EAP;
		}
		return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3;
	}

	protected String getHost(IRoute route) {
		if (route == null) {
			return "";
		}
		return UrlUtils.getHost(route.getURL());
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
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.useInferredPodPath,
				getService(), getServiceItems(),
				this.route, selectDefaultRoute, this.routesByProject,
				this.ocBinaryStatus);
	}

	protected void setRoutes(Map<IProject, List<IRoute>> routesByProject) {
		updateRoutes(service, routesByProject);
	}

	/**
	 * Returns the routes that match the service that's currently selected.
	 * 
	 * @return the routes that match the selected service
	 * 
	 * @see #getService()
	 * @see #getAllRoutes(IService)
	 */
	public List<IRoute> getRoutes() {
		if (getService() == null) {
			return Collections.emptyList();
		}
		return getRoutes(getService());
	}

	protected List<IRoute> getRoutes(IService service) {
		return ResourceUtils.getRoutesForService(service, getAllRoutes(service));
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
				newRoute, this.selectDefaultRoute, this.routesByProject,
				this.ocBinaryStatus);
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
	
	protected String getConnectionUrl(IConnection connection) {
		ConnectionURL connectionUrl;
		try {
			connectionUrl = ConnectionURL.forConnection(connection);
			return connectionUrl.toString();
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftException(e, "Could not get url for connection {0}", connection.getHost());
		}
	}

    private void updateOCBinaryStatus(IStatus ocBinaryStatus) {
        firePropertyChange(PROPERTY_OC_BINARY_STATUS, this.ocBinaryStatus,
                this.ocBinaryStatus = ocBinaryStatus);
    }

    public IStatus getOCBinaryStatus() {
        return ocBinaryStatus;
    }

    public void setOCBinaryStatus(IStatus ocBinaryStatus) {
        update(getConnection(), getConnections(), 
                this.deployProject, this.projects, 
                this.sourcePath, this.podPath, this.useInferredPodPath,
                getService(), getServiceItems(),
                route, this.selectDefaultRoute, this.routesByProject,
                ocBinaryStatus);
    }
    
    protected IServerWorkingCopy getServer() {
    	return server;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        boolean[] needReload = new boolean[1];
        try {
            event.getDelta().accept((delta) -> {
              if ((delta.getResource().getType() == org.eclipse.core.resources.IResource.PROJECT) &&
                  ((delta.getKind() == IResourceDelta.ADDED) || (delta.getKind() == IResourceDelta.REMOVED))) {
                needReload[0] = true;  
              }
              return true;
            }, true);
            if (needReload[0]) {
              setProjects(loadProjects());  
            }
        }
        catch (CoreException e) {
            OpenShiftUIActivator.log(Status.ERROR, e.getLocalizedMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.common.databinding.ObservablePojo#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }
}
