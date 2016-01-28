/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.server.ServerUtils;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.util.CollectionUtils;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.DeploymentResourceMapper;
import org.jboss.tools.openshift.internal.ui.models.IDeploymentResourceMapper;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.treeitem.IModelFactory;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsViewModel extends ServiceViewModel {

	public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
	public static final String PROPERTY_PROJECTS = "projects";
	public static final String PROPERTY_SOURCE_PATH = "sourcePath";
	public static final String PROPERTY_POD_PATH = "podPath";

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
	private IServerWorkingCopy server;
	private Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName;

	public ServerSettingsViewModel(IServerWorkingCopy server, Connection connection) {
		super(connection);
		this.server = server;
	}

	protected void update(Connection connection, List<Connection> connections, 
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName, 
			IService service, List<ObservableTreeItem> serviceItems) {
		update(connection, connections, service, serviceItems);
		updateProjects(projects);
		if (this.deployProject != deployProject) {
			//project changed, reset default sourcePath
			sourcePath = null;
		}
		deployProject = updateDeployProject(deployProject, projects);
		updateSourcePath(sourcePath, deployProject);
		updatePodPath(podPath, deploymentMapperByProjectName, service);
	}

	private void updateProjects(List<org.eclipse.core.resources.IProject> projects) {
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

	protected void updateSourcePath(String sourcePath, org.eclipse.core.resources.IProject deployProject) {
		if (StringUtils.isEmpty(sourcePath)
				&& ProjectUtils.isAccessible(deployProject)) {
			String projectPath = deployProject.getFullPath().toString();
			sourcePath = VariablesHelper.addWorkspacePrefix(projectPath);
		}
		firePropertyChange(PROPERTY_SOURCE_PATH, this.sourcePath, this.sourcePath = sourcePath);
	}

	protected void updatePodPath(String newPodPath, Map<String, IDeploymentResourceMapper> deploymentResourceMapperByProjectName, IService service) {
		this.deploymentMapperByProjectName = deploymentResourceMapperByProjectName;
		newPodPath = getDeploymentDirectory(service, deploymentResourceMapperByProjectName);
		firePropertyChange(PROPERTY_POD_PATH, this.podPath, this.podPath = newPodPath);
	}

	private String getDeploymentDirectory(IService service, Map<String, IDeploymentResourceMapper> deploymentResourceMapperByProjectName) {
		String deploymentDirectory = null;
		if (service != null
				&& deploymentResourceMapperByProjectName != null) {
			IProject project = service.getProject();
			IDeploymentResourceMapper deploymentMapper = deploymentResourceMapperByProjectName.get(project.getName());
			if (deploymentMapper != null) {
				Collection<IResource> istags = getImageStreamTags(service, deploymentMapper);
				Iterator<IResource> istagsIterator = istags.iterator();
				if (istagsIterator.hasNext()) {
					IResource imageStreamTag = istagsIterator.next();
					String imageStreamTagJson = imageStreamTag.toJson(true);
					if ((deploymentDirectory = matchFirstGroup(imageStreamTagJson, PATTERN_REDHAT_DEPLOYMENTS_DIR)) == null) {
						if ((deploymentDirectory = matchFirstGroup(imageStreamTagJson, PATTERN_JBOSS_DEPLOYMENTS_DIR)) == null) {
							deploymentDirectory = matchFirstGroup(imageStreamTagJson, PATTERN_WOKRING_DIR);
						}
					}
				}
			}
		}

		if (StringUtils.isEmpty(deploymentDirectory)) {
			deploymentDirectory = DEFAULT_DEPLOYMENT_DIR;
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

	private Collection<IResource> getImageStreamTags(IService service, IDeploymentResourceMapper deploymentMapper) {
		Optional<Deployment> deployment = deploymentMapper.getDeployments().stream().filter(d->service.equals(d.getService())).findFirst();
		if(deployment.isPresent()) {
			Collection<IResource> istags = deployment.get().getImageStreamTags().stream().map(m->m.getResource()).collect(Collectors.toList());
			// need to refresh to get full resource WITH labels 
			return getVerboseResources(istags, getConnection());
		}
		return Collections.emptyList();
	}

	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		update(getConnection(), getConnections(), project, this.projects, this.sourcePath, 
				this.podPath, this.deploymentMapperByProjectName, getService(), getServiceItems());
	}

	public org.eclipse.core.resources.IProject getDeployProject() {
		return this.deployProject;
	}

	protected void setProjects(List<org.eclipse.core.resources.IProject> projects) {
		update(getConnection(), getConnections(), this.deployProject, projects, this.sourcePath, 
				this.podPath, this.deploymentMapperByProjectName, getService(), getServiceItems());
	}

	public List<org.eclipse.core.resources.IProject> getProjects() {
		return projects;
	}
	
	public void setSourcePath(String sourcePath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, sourcePath, 
				this.podPath, this.deploymentMapperByProjectName, getService(), getServiceItems());
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setPodPath(String podPath) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, 
				podPath, this.deploymentMapperByProjectName, getService(), getServiceItems());
	}

	public String getPodPath() {
		return podPath;
	}

	@Override
	public void setService(IService service) {
		update(getConnection(), getConnections(), 
				this.deployProject, this.projects, 
				this.sourcePath, podPath, deploymentMapperByProjectName, 
				service, getServiceItems());
	}
	
	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		if (project == null) {
			project = CollectionUtils.getFirstElement(projects);
		}
		return project;
	}

	@Override
	public void loadResources() {
		super.loadResources();
		setProjects(loadProjects());
		setImageStreamTags(loadImageStreamTags());
	}

	private void setImageStreamTags(Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, podPath, deploymentMapperByProjectName, getService(), getServiceItems());
	}

	protected Map<String, IDeploymentResourceMapper> getImageStreamTags(){
		return deploymentMapperByProjectName;
	}
	
	private Map<String, IDeploymentResourceMapper> loadImageStreamTags() {
		Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName = new HashMap<>();
		Connection connection = getConnection();
		if (connection != null) {
			List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
			if (projects != null) {
				projects.forEach(project -> {
					IDeploymentResourceMapper deploymentMapper = new DeploymentResourceMapper(connection,
							new ProjectAdapterFake((IProject) project));
					deploymentMapper.refresh();
					deploymentMapperByProjectName.put(project.getName(), deploymentMapper);
				});
			}
		}
		return deploymentMapperByProjectName;
	}

	/**
	 * Returns the verbose version of the resources by requesting them individually. 
	 * When listing all resources you only get a compact short version. 
	 * To get the full-blown resource WITH labels etc you have to query them individually.
	 * 
	 * @param resources
	 * @param connection
	 * @return
	 */
	private List<IResource> getVerboseResources(Collection<IResource> resources, Connection connection) {
		if (resources == null
				|| resources.isEmpty()) {
			return Collections.emptyList();
		}
		return resources.stream().map(r -> (IResource) connection.getResource(r)).collect(Collectors.toList());
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
	}

	protected List<ObservableTreeItem> loadServices(Connection connection) {
		ObservableTreeItem connectionItem = ServiceTreeItemsFactory.INSTANCE.create(connection);
		connectionItem.load();
		return connectionItem.getChildren();
	}

	public void updateServer() {
		updateServer(server);
	}
	
	private void updateServer(IServerWorkingCopy server) throws OpenShiftException {
		String connectionUrl = getConnectionUrl(getConnection());
		//Compute default name
		String baseServerName = OpenShiftServerUtils.getServerName(getService(), getConnection());
		//Find a free name based on the computed name
		String serverName = ServerUtils.getServerName(baseServerName);
		OpenShiftServerUtils.updateServer(
				serverName, connectionUrl, getService(), sourcePath, podPath, deployProject, server);
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, getService(), sourcePath, podPath, deployProject);
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
	
	protected IServerWorkingCopy getServer() {
		return server;
	}

	static class ServiceTreeItemsFactory implements IModelFactory {

		private static final ServiceTreeItemsFactory INSTANCE = new ServiceTreeItemsFactory();
			
		@SuppressWarnings("unchecked")
		public <T> List<T> createChildren(Object parent) {
			if (parent instanceof Connection) {
				return (List<T>) ((Connection) parent).getResources(ResourceKind.PROJECT);
			} else if (parent instanceof IProject) {
				return (List<T>) ((IProject) parent).getResources(ResourceKind.SERVICE);
			}
			return Collections.emptyList();
		}

		public List<ObservableTreeItem> create(Collection<?> openShiftObjects) {
			if (openShiftObjects == null) {
				return Collections.emptyList();
			}
			List<ObservableTreeItem> items = new ArrayList<>();
			for (Object openShiftObject : openShiftObjects) {
				ObservableTreeItem item = create(openShiftObject);
				if (item != null) {
					items.add(item);
				}
			}
			return items;
		}

		public ObservableTreeItem create(Object object) {
			return new ObservableTreeItem(object, this);
		}
	}

	public static class ProjectAdapterFake implements IProjectAdapter {

		private IProject project;

		private ProjectAdapterFake(IProject project) {
			this.project = project;
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
		
	}
	
}
