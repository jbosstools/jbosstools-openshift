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

import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public static final String PROPERTY_POD_PATH_EDITABLE = "podPathEditable";

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
	private Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName;

	public ServerSettingsViewModel(IServerWorkingCopy server, Connection connection) {
		super(connection);
		this.server = server;
	}

	protected void update(Connection connection, List<Connection> connections, 
			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
			String sourcePath, String podPath, Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName, 
			IService service, List<ObservableTreeItem> serviceItems) {
		boolean serviceOrDeploymentChanged = this.deploymentMapperByProjectName != deploymentMapperByProjectName;
		IService oldService = getService();
		update(connection, connections, service, serviceItems);
		serviceOrDeploymentChanged |= (oldService != getService());
		updateProjects(projects);
		if (this.deployProject != deployProject) {
			//project changed, reset default sourcePath
			sourcePath = null;
		}
		deployProject = updateDeployProject(deployProject, projects);
		updateSourcePath(sourcePath, deployProject);
		if(serviceOrDeploymentChanged) {
			updatePodPath(podPath, deploymentMapperByProjectName, service);
		} else {
			updatePodPath(podPath);
		}
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
		String inferredPodPath = getInferredDeploymentDirectory(service, deploymentResourceMapperByProjectName, newPodPath);
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

	private String getInferredDeploymentDirectory(IService service, Map<String, IDeploymentResourceMapper> deploymentResourceMapperByProjectName, String selectedDeploymentDirectory) {
		String deploymentDirectory = null;
		if (service != null
				&& deploymentResourceMapperByProjectName != null) {
			IDeploymentResourceMapper deploymentMapper = deploymentResourceMapperByProjectName.get(service.getNamespace());
			if (deploymentMapper != null) {
				for (Iterator<IResource> istagsIterator = deploymentMapper.getImageStreamTagsFor(service).iterator(); 
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

	public boolean isPodPathEditable() {
		return inferredPodPath == null;
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
		loadResources(getConnection());
	}

	public void loadResources(Connection newConnection) {
		super.loadResources(newConnection);
		setProjects(loadProjects());
		setDeploymentMappers(createDeploymentMappers(newConnection));
	}

	private void setDeploymentMappers(Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName) {
		update(getConnection(), getConnections(), this.deployProject, this.projects, this.sourcePath, podPath, deploymentMapperByProjectName, getService(), getServiceItems());
	}

	protected Map<String, IDeploymentResourceMapper> getImageStreamTags(){
		return deploymentMapperByProjectName;
	}
	
	private Map<String, IDeploymentResourceMapper> createDeploymentMappers(Connection connection) {
		Map<String, IDeploymentResourceMapper> deploymentMapperByProjectName = new HashMap<>();
		if (connection != null) {
			List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
			if (projects != null) {
				projects.forEach(project -> {
					IDeploymentResourceMapper deploymentMapper = 
							new DeploymentResourceMapper(connection, new ProjectAdapterFake((IProject) project));
					deploymentMapper.refresh();
					deploymentMapperByProjectName.put(project.getName(), deploymentMapper);
				});
			}
		}
		return deploymentMapperByProjectName;
	}

	protected List<org.eclipse.core.resources.IProject> loadProjects() {
		return ProjectUtils.getAllAccessibleProjects();
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
		String routeURL = isSelectDefaultRoute() && getRoute() != null ? getRoute().getURL() : null;
		OpenShiftServerUtils.updateServer(
				serverName, connectionUrl, getService(), sourcePath, podPath, deployProject, routeURL, server);
		OpenShiftServerUtils.updateServerProject(
				connectionUrl, getService(), sourcePath, podPath, routeURL, deployProject);
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

	private static class ProjectAdapterFake implements IProjectAdapter {

		private IProject project;

		private ProjectAdapterFake(IProject project) {
			this.project = project;
		}

		
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
