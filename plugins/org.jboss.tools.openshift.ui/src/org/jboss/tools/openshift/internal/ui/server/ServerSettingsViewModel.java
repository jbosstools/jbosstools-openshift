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

import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;
import org.jboss.tools.openshift.internal.core.ImportImageMetaData;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.DeploymentResourceMapper;
import org.jboss.tools.openshift.internal.ui.models.IDeploymentResourceMapper;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.utils.ObservableTreeItemUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.image.IImageStreamImport;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsViewModel extends ServiceViewModel {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";
	public static final String PROPERTY_POD_PATH_EDITABLE = "podPathEditable";

	public static final String PROPERTY_SELECT_DEFAULT_ROUTE = "selectDefaultRoute";
	public static final String PROPERTY_ROUTE = "route";
	public static final String PROPERTY_ROUTES = "routes";

	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_REDHAT_DEPLOYMENTS_DIR = Pattern.compile("\"com\\.redhat\\.deployments-dir\"[^\"]*\"([^\"]*)\","); 
	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_JBOSS_DEPLOYMENTS_DIR = Pattern.compile("\"org\\.jboss\\.deployments-dir\"[^\"]*\"([^\"]*)\","); 
	// "image->"dockerImageMetadata"->"Config"->"WorkginDir"
	private static final Pattern PATTERN_WOKRING_DIR = Pattern.compile("\"WorkingDir\"[^\"]*\"([^\"]*)\",");
	// default fallback 
	private static final String DEFAULT_DEPLOYMENT_DIR = "/opt/app-root/src";
	
	protected org.eclipse.core.resources.IProject deployProject;
	protected List<org.eclipse.core.resources.IProject> projects = new ArrayList<>();
	private String sourcePath;
	protected String podPath;
	protected String inferredPodPath = null;
	private IServerWorkingCopy server;
	private Map<String, ProjectImageStreamTags> imageStreamTagsMap;
	private boolean selectDefaultRoute = false;
	private IRoute route;
	private Map<IProject, List<IRoute>> routesByProject = new HashMap<>();
	private boolean isLoaded = false;
	private Map<IProject, List<IBuildConfig>> buildConfigsByProject;

	public ServerSettingsViewModel(IServerWorkingCopy server, Connection connection) {
		this(null, null, server, connection);
	}

	public ServerSettingsViewModel(IService service, IRoute route, IServerWorkingCopy server, Connection connection) {
		super(service, connection);
		this.route = route;
		this.server = server;
	}

	protected void update(Connection connection, List<Connection> connections, 
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, Map<String, ProjectImageStreamTags> imageStreamsMap, 
			IService service, List<ObservableTreeItem> serviceItems, 
			IRoute route, boolean isSelectDefaultRoute, Map<IProject, List<IRoute>> routesByProject) {
		boolean serviceOrDeploymentChanged = this.imageStreamTagsMap != imageStreamsMap;
		IService oldService = getService();
		update(connection, connections, service, serviceItems);
		serviceOrDeploymentChanged |= (oldService != getService());
		updateProjects(projects);
		org.eclipse.core.resources.IProject oldDeployProject = this.deployProject;
		org.eclipse.core.resources.IProject newDeployProject = updateDeployProject(deployProject, projects);
		updateSourcePath(sourcePath, newDeployProject, oldDeployProject);
		if(serviceOrDeploymentChanged) {
			updatePodPath(podPath, imageStreamsMap, service);
		} else {
			updatePodPath(podPath);
		}
		List<IRoute> newRoutes = updateRoutes(service, routesByProject);
		updateRoute(route, newRoutes, service);
		updateSelectDefaultRoute(isSelectDefaultRoute);
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

	protected org.eclipse.core.resources.IProject updateDeployProject(org.eclipse.core.resources.IProject deployProject,
			List<org.eclipse.core.resources.IProject> projects) {
		firePropertyChange(PROPERTY_DEPLOYPROJECT, this.deployProject, this.deployProject = getProjectOrDefault(deployProject, projects));
		return this.deployProject;
	}

	protected void updateSourcePath(String sourcePath, org.eclipse.core.resources.IProject newDeployProject, org.eclipse.core.resources.IProject oldDeployProject) {
		if ((StringUtils.isEmpty(sourcePath)
				|| newDeployProject != oldDeployProject)
				&& ProjectUtils.isAccessible(newDeployProject)) {
			String projectPath = newDeployProject.getFullPath().toString();
			sourcePath = VariablesHelper.addWorkspacePrefix(projectPath);
		}
		firePropertyChange(PROPERTY_SOURCE_PATH, this.sourcePath, this.sourcePath = sourcePath);
	}

	protected void updatePodPath(String newPodPath, Map<String, ProjectImageStreamTags> imageStreamsMap, IService service) {
		this.imageStreamTagsMap = imageStreamsMap;
		String inferredPodPath = getInferredDeploymentDirectory(service, imageStreamsMap, newPodPath);
		firePropertyChange(PROPERTY_POD_PATH_EDITABLE, this.inferredPodPath == null, (this.inferredPodPath = inferredPodPath) == null);
		if(inferredPodPath != null) {
			newPodPath = inferredPodPath;
		} else if (newPodPath == null) {
			newPodPath = DEFAULT_DEPLOYMENT_DIR;
		}
		updatePodPath(newPodPath);
	}

	//Should only be called after newPodPath is revalidated against directories 
	//inferred from image stream tags.
	private void updatePodPath(String newPodPath) {
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = newPodPath);
	}

	private String getInferredDeploymentDirectory(IService service, Map<String, ProjectImageStreamTags> imageStreamsMap, String selectedDeploymentDirectory) {
		String deploymentDirectory = null;
		if (service != null
				&& imageStreamsMap != null) {
			ProjectImageStreamTags imageStreams = imageStreamsMap.get(service.getNamespace());
			if (imageStreams != null) {
				for (Iterator<IResource> istagsIterator = imageStreams.getImageStreamTags(service).iterator(); 
						istagsIterator.hasNext();) {
					String imageStreamTag = istagsIterator.next().toJson(true);
					String dir = null;
					if ((dir = matchFirstGroup(imageStreamTag, PATTERN_REDHAT_DEPLOYMENTS_DIR)) == null) {
						if ((dir = matchFirstGroup(imageStreamTag, PATTERN_JBOSS_DEPLOYMENTS_DIR)) == null) {
							dir = matchFirstGroup(imageStreamTag, PATTERN_WOKRING_DIR);
						}
					}
					if (dir != null) {
						if (dir.equals(selectedDeploymentDirectory)) {
							deploymentDirectory = dir;
							// The selected directory is inferred. Do not look
							// for another.
							break;
						} else if (deploymentDirectory == null) {
							// Remember the first one found. Continue to look
							// for coincidence with selected directory.
							deploymentDirectory = dir;
						}
					}
				}
			}
		}

		return deploymentDirectory;
	}

	private String matchFirstGroup(String imageStreamTag, Pattern pattern) {
		Matcher matcher = pattern.matcher(imageStreamTag);
		if (matcher.find()
				&& matcher.groupCount() == 1) {
			return matcher.group(1);	
		} else {
			return null;
		}
		
	}

	/**
	 * Replaces choices in the route selector as needed.
	 * If choices are replaced calls updateRoute() to reset its selected value.
	 * @param routesByProject2 
	 * @param service 
	 * @return 
	 */
	protected List<IRoute> updateRoutes(IService service, Map<IProject, List<IRoute>> routesByProject) {
		if (this.routesByProject.equals(routesByProject)) {
			return getAllRoutes(service);
		}
		
		this.routesByProject.clear();
		this.routesByProject.putAll(routesByProject);

		List<IRoute> routes = getAllRoutes(service);
		firePropertyChange(PROPERTY_ROUTES, null, routes);
		return routes;
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

	public void setDeployProject(IService service, List<IBuildConfig> buildConfigs, List<org.eclipse.core.resources.IProject> workspaceProjects) {
		IBuildConfig buildConfig = ResourceUtils.getBuildConfigForService(service, buildConfigs);
		org.eclipse.core.resources.IProject deployProject = ResourceUtils.getWorkspaceProjectForBuildConfig(buildConfig, getProjects());
		setDeployProject(deployProject);
	}

	protected void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), 
				project, this.projects, 
				this.sourcePath, this.podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject);
	}

	public org.eclipse.core.resources.IProject getDeployProject() {
		return this.deployProject;
	}

	protected void setProjects(List<org.eclipse.core.resources.IProject> projects) {
		update(getConnection(), getConnections(), 
				this.deployProject, projects, 
				this.sourcePath, this.podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject);
	}

	public List<org.eclipse.core.resources.IProject> getProjects() {
		return projects;
	}
	
	public void setSourcePath(String sourcePath) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				sourcePath, this.podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setPodPath(String podPath) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject);
	}

	public String getPodPath() {
		return podPath;
	}

	public boolean isPodPathEditable() {
		return inferredPodPath == null;
	}

	@Override
	public void setService(IService service) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, podPath, imageStreamTagsMap, 
				service, getServiceItems(), 
				this.route, this.selectDefaultRoute, this.routesByProject);
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

	public void loadResources(Connection newConnection) {
		this.isLoaded = false;
		
		super.loadResources(newConnection);
		setProjects(loadProjects());
		List<IProject> openshiftProjects = ObservableTreeItemUtils.getAllModels(IProject.class, getServiceItems());
		setBuildConfigs(loadBuildConfigs(openshiftProjects, newConnection));
		List<IBuildConfig> buildConfigs = getBuildConfigs(getOpenShiftProject(service));
		setDeployProject(service, buildConfigs, getProjects());
		setImageStreamTags(loadImageStreamTags(openshiftProjects, newConnection));
		setRoutes(loadRoutes(getServiceItems()));

		this.isLoaded = true;
	}

	private List<IBuildConfig> getBuildConfigs(IProject project) {
		if (buildConfigsByProject == null) {
			return null;
		}
		return buildConfigsByProject.get(project);
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

	private void setImageStreamTags(Map<String, ProjectImageStreamTags> imageStreamsMap) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, imageStreamsMap, 
				getService(), getServiceItems(),
				this.route, this.selectDefaultRoute, this.routesByProject);
	}

	protected Map<String, ProjectImageStreamTags> getImageStreamTagsMap(){
		return imageStreamTagsMap;
	}
	
	private Map<String, ProjectImageStreamTags> loadImageStreamTags(List<IProject> projects, Connection connection) {
		if (connection != null
				&& projects != null) {
					projects.stream().collect(
							Collectors.toMap(
									project -> project,
									project -> {
											IImageStreamImportCapability imageStreamTags = 
												((IProject) project).getCapability(IImageStreamImportCapability.class);
											List<IBuildConfig> buildConfigs = getBuildConfigs((IProject) project);
											List<String> imageURIs = buildConfigs.stream()
													.map(bc -> ResourceUtils.imageRef(bc))
													.collect(Collectors.toList());
											return getImageMetaData(imageStreamTags, imageURIs);
									}));
				
				return projects.stream()
						.collect(Collectors.toMap(
								project -> project.getName(), 
								project -> new ProjectImageStreamTags(project, connection))
				);
			}
		return Collections.emptyMap();

	}

	private List<ImportImageMetaData> getImageMetaData(IImageStreamImportCapability imageStreamTags, List<String> imageURIs) {
		return imageURIs.stream()
			.map(imageURI -> {
					DockerImageURI uri = new DockerImageURI(imageURI);
					IImageStreamImport imageStreamImport = imageStreamTags.importImageMetadata(uri);
					if (ResourceUtils.isSuccessful(imageStreamImport)) {
						return new ImportImageMetaData(((IImageStreamImport) imageStreamImport).getImageJsonFor(uri));
					} else {
						return null;
					}
				})
			.filter(metaData -> metaData != null)
			.collect(Collectors.toList());
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
	}
	
	protected Map<IProject, List<IRoute>> loadRoutes(List<ObservableTreeItem> serviceItems) {
		List<IProject> projects = ObservableTreeItemUtils.getAllModels(IProject.class, serviceItems);
		return projects.stream()
				.collect(Collectors.toMap(project -> project, project -> project.getResources(ResourceKind.ROUTE)));
	}

	public void updateServer() {
		updateServer(server);
	}
	
	public void updateServer(String attribute, Object value) {
		updateServer(server);
	}

	private void updateServer(IServerWorkingCopy server) throws OpenShiftException {
		String connectionUrl = getConnectionUrl(getConnection());
		
		//Compute default name
		String baseServerName = OpenShiftServerUtils.getServerName(getService(), getConnection());
		//Find a free name based on the computed name
		String serverName = ServerUtils.getServerName(baseServerName);
		String routeURL = getRoute(isSelectDefaultRoute(), getRoute());
		OpenShiftServerUtils.updateServer(
				serverName, connectionUrl, getService(), sourcePath, podPath, deployProject, routeURL, server);
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
		// currently supported profiles are openshift3   or   openshift3.eap
		// we need to determine if the current service represents an app that requires
		// eap-style behavior or normal behavior
		String template = getService().getLabels().get("template");
		if( template != null && template.startsWith("eap")) {
			return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3_EAP;
		}
		return OpenShiftServerBehaviour.PROFILE_OPENSHIFT3;
	}

	private String getRoute(boolean isDefaultRoute, IRoute route) {
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
				this.sourcePath, podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(),
				this.route, selectDefaultRoute, this.routesByProject);
	}

	protected void setRoutes(Map<IProject, List<IRoute>> routesByProject) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, this.podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(),
				this.route, this.selectDefaultRoute, routesByProject);
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
				this.sourcePath, podPath, this.imageStreamTagsMap, 
				getService(), getServiceItems(),
				route, this.selectDefaultRoute, this.routesByProject);
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

	public static class ProjectImageStreamTags {

		private Map<IService, Collection<IResource>> imageStreamTagsByService;

		public ProjectImageStreamTags(IProject project, Connection connection) {
			this.imageStreamTagsByService = createImageStreamsMap(project, connection);
		}
		
		private Map<IService, Collection<IResource>> createImageStreamsMap(IProject project, Connection connection) {
			IDeploymentResourceMapper deploymentMapper = 
					new DeploymentResourceMapper(connection, new ProjectAdapterFake((IProject) project));
			deploymentMapper.refresh();
			return deploymentMapper.getAllImageStreamTags();
		}
		
		public Collection<IResource> getImageStreamTags(IService service) {
			return imageStreamTagsByService.get(service);
		}
	}
	
	private static class ProjectAdapterFake implements IProjectAdapter {

		private IProject project;

		private ProjectAdapterFake(IProject project) {
			this.project = project;
		}
		
		@Override
		public void dispose() {
		}
		
		
		@Override
		public void setDeleting(boolean deleting) {
		}

		@Override
		public boolean isDeleting() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Collection<IResourceUIModel> getBuilds() {
			return null;
		}

		@Override
		public void setBuilds(Collection<IResourceUIModel> builds) {
		}

		@Override
		public void setBuildResources(Collection<IBuild> builds) {
		}

		@Override
		public Collection<IResourceUIModel> getImageStreams() {
			return null;
		}

		@Override
		public void setImageStreams(Collection<IResourceUIModel> models) {
		}

		@Override
		public void setImageStreamResources(Collection<IResource> streams) {
		}

		@Override
		public Collection<IResourceUIModel> getDeploymentConfigs() {
			return null;
		}

		@Override
		public void setDeploymentConfigs(Collection<IResourceUIModel> models) {
		}

		@Override
		public void setDeploymentConfigResources(Collection<IResource> dcs) {
		}

		@Override
		public Collection<IResourceUIModel> getPods() {
			return null;
		}

		@Override
		public void setPods(Collection<IResourceUIModel> pods) {
		}

		@Override
		public void setPodResources(Collection<IPod> pods) {
		}

		@Override
		public Collection<IResourceUIModel> getRoutes() {
			return null;
		}

		@Override
		public void setRoutes(Collection<IResourceUIModel> routes) {
		}

		@Override
		public void setRouteResources(Collection<IResource> routes) {
		}

		@Override
		public Collection<IResourceUIModel> getReplicationControllers() {
			return null;
		}

		@Override
		public void setReplicationControllers(Collection<IResourceUIModel> rcs) {
		}

		@Override
		public void setReplicationControllerResources(Collection<IResource> rcs) {
		}

		@Override
		public Collection<IResourceUIModel> getBuildConfigs() {
			return null;
		}

		@Override
		public void setBuildConfigs(Collection<IResourceUIModel> buildConfigs) {
			
		}

		@Override
		public void setBuildConfigResources(Collection<IResource> buildConfigs) {
		}

		@Override
		public Collection<IResourceUIModel> getServices() {
			return null;
		}

		@Override
		public void setServices(Collection<IResourceUIModel> services) {
		}

		@Override
		public void setServiceResources(Collection<IResource> services) {
		}

		@Override
		public void add(IResource resource) {
		}

		@Override
		public void update(IResource resource) {
		}

		@Override
		public void remove(IResource resource) {
		}

		@Override
		public Object getParent() {
			return null;
		}

		@Override
		public void refresh() {
		}

		@Override
		public IProject getProject() {
			return project;
		}

		@Override
		public <T extends IResource> void setResources(Collection<T> resources, String kind) {
		}

		@Override
		public Collection<Deployment> getDeployments() {
			return null;
		}

		@Override
		public void setDeployments(Collection<Deployment> deployment) {
		}

		@Override
		public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		}

		@Override
		public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		}
		
	}

}
