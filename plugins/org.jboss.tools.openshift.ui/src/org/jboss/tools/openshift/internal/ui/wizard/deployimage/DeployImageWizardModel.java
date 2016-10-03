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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerConnectionManagerListener2;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.core.IDockerImageMetadata;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.util.OpenShiftProjectUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dockerutils.DockerImageUtils;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablesPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IServicePort;

/**
 * The Wizard model to support deploying an image to OpenShift
 * @author jeff.cantrill
 *
 */
public class DeployImageWizardModel 
		extends ResourceLabelsPageModel 
		implements IDeployImageParameters, IDockerConnectionManagerListener2, PropertyChangeListener{

	private static final int DEFAULT_REPLICA_COUNT = 1;

	private Connection connection;
	private IProject project;
	private String resourceName;
	private String imageName;
	private List<IProject> projects = Collections.emptyList();

	private EnvironmentVariablesPageModel envModel = new EnvironmentVariablesPageModel();

	private List<String> volumes = Collections.emptyList();
	private String selectedVolume;

	private List<IPort> portSpecs = Collections.emptyList();

	private int replicas;

	private boolean addRoute = true;

	List<IServicePort> servicePorts = new ArrayList<>();
	IServicePort selectedServicePort = null;
	
	private IDockerConnection dockerConnection;
	private ArrayList<IServicePort> imagePorts;
	private boolean originatedFromDockerExplorer;
	private boolean isStartedWithActiveConnection = false;
	private IDockerImageMetadata imageMeta;
	private boolean pushImageToRegistry = false;
	private String targetRegistryLocation;
	private String targetRegistryUsername;
	private String targetRegistryPassword;

	
	
	private static final DockerImage2OpenshiftResourceConverter dockerImage2OpenshiftResourceConverter = new DockerImage2OpenshiftResourceConverter();
	private final List<String> imageNames = new ArrayList<>();
    private List<IDockerConnection> dockerConnections = Arrays.asList(DockerConnectionManager.getInstance().getConnections());
    private Comparator<IProject> projectsComparator;

	public DeployImageWizardModel() {
		envModel.addPropertyChangeListener(PROPERTY_ENVIRONMENT_VARIABLES, this);
		envModel.addPropertyChangeListener(PROPERTY_SELECTED_ENVIRONMENT_VARIABLE, this);
		DockerConnectionManager.getInstance().addConnectionManagerListener(this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
	    DockerConnectionManager.getInstance().removeConnectionManagerListener(this);
	    ((EnvironmentVariablesPageModel)envModel).dispose();
	    connection = null;
	    project = null;
	    dockerConnection = null;
	    projects.clear();
	    volumes.clear();
	    portSpecs.clear();
	    if(imagePorts != null) {
	    	imagePorts.clear();
	    	imagePorts = null;
	    }
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt != null) {
			firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		}
	}

	@Override
	public void setOriginatedFromDockerExplorer(boolean orig) {
		this.originatedFromDockerExplorer = orig;
	}

	@Override
	public boolean originatedFromDockerExplorer() {
		return originatedFromDockerExplorer;
	}

	@Override
	public boolean isStartedWithActiveConnection() {
		return isStartedWithActiveConnection;
	}

	public void setStartedWithActiveConnection(boolean active) {
		isStartedWithActiveConnection = active;
	}

	@Override
	public List<IDockerConnection> getDockerConnections() {
		return dockerConnections;
	}

	@Override
	public IDockerConnection getDockerConnection() {
		if(dockerConnection == null) {
			List<IDockerConnection> all = getDockerConnections();
			if(all.size() == 1) {
				setDockerConnection(all.get(0));
			}
		}
		return dockerConnection;
	}

	@Override
	public Collection<Connection> getConnections() {
		return ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
	}

	@Override
	public Connection getConnection() {
		if (connection == null) {
			Collection<Connection> connections = getConnections();
			if (connections.size() == 1) {
				setConnection(connections.iterator().next());
			}
		}
		return connection;
	}
	
	/**
	 * Loads all projects for the given {@code connection} and sets this model's project from the given {@code project}.
	 * @param connection the connection from which the projects will be retrieved
	 * @param project the project to set in the model
	 */
	public void initModel(final Connection connection, final IProject project, boolean loadResources) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		if(connection == null) {
			return;
		}

		initModelRegistry(connection);

		if (loadResources) {
			Job job = new AbstractDelegatingMonitorJob("Loading projects...") {
	
				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
						setProjects(projects);
						setProjectOrDefault(project);
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Unable to load the OpenShift projects for the selected connection.", e);
					}
				}
	
			};
			job.schedule();
		}
	}

	private void initModelRegistry(Connection connection) {
		if(connection != null) {
			setTargetRegistryLocation(
				(String) connection.getExtendedProperties().get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY));
			setTargetRegistryUsername(connection.getUsername());
			setTargetRegistryPassword(connection.getToken());
		}
	}

	@Override
	public void setConnection(final Connection connection) {
		initModel(connection, null, true);
	}

	@Override
	public void setProjects(List<IProject> projects) {
		if(projects == null) projects = Collections.emptyList();
		firePropertyChange(PROPERTY_PROJECTS, this.projects, this.projects = projects);
		if(!projects.isEmpty() && !projects.contains(getProject())) {
			setProject(null);
		}
		if(projects.size() == 1) {
			setProject(projects.iterator().next());
		}
	}

	@Override
	public List<IProject> getProjects() {
		return projects;
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public void setProject(IProject project) {
		firePropertyChange(PROPERTY_PROJECT, this.project, this.project = project);
	}

	protected void setProjectOrDefault(IProject project) {
		if (projects == null) {
			return;
		}

		if (project != null
				&& projects.contains(project)) {
			setProject(project);
		} else if (!projects.isEmpty()) {
			project = getDefaultProject();
			setProject(project);
		}
	}

	private IProject getDefaultProject() {
		IProject project;
		if (projectsComparator != null) {
			Collections.sort(projects, projectsComparator);
			project = projects.get(0);
		} else {
			project = projects.get(0);
		}
		return project;
	}

	@Override
	public void setProjectsComparator(Comparator<IProject> comparator) {
		this.projectsComparator = comparator;
	}
	
	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public void setResourceName(String resourceName) {
		firePropertyChange(PROPERTY_RESOURCE_NAME, this.resourceName, this.resourceName = resourceName);
	}

	@Override
	public String getImageName() {
		return this.imageName;
	}

	@Override
	public void setImageName(final String imageName) {
		if(StringUtils.isBlank(imageName)) {
			return;
		}
		firePropertyChange(PROPERTY_IMAGE_NAME, this.imageName, this.imageName = imageName);
		final DockerImageURI uri = new DockerImageURI(imageName);
		setResourceName(dockerImage2OpenshiftResourceConverter.convert(uri));
	}

	@Override
	public void setImageName(String imageName, boolean forceUpdate) {
		if(forceUpdate && this.imageName != null && this.imageName.equals(imageName)) {
			firePropertyChange(PROPERTY_IMAGE_NAME, this.imageName, this.imageName = null);
		}
		setImageName(imageName);
	}

	@Override
	public boolean isPushImageToRegistry() {
		return this.pushImageToRegistry;
	}

	@Override
	public void setPushImageToRegistry(final boolean pushImageToRegistry) {
		firePropertyChange(PROPERTY_PUSH_IMAGE_TO_REGISTRY, this.pushImageToRegistry,
				this.pushImageToRegistry = pushImageToRegistry);
	}
	
	@Override
	public String getTargetRegistryLocation() {
		return this.targetRegistryLocation;
	}
	
	@Override
	public void setTargetRegistryLocation(final String targetRegistryLocation) {
		firePropertyChange(PROPERTY_TARGET_REGISTRY_LOCATION, this.targetRegistryLocation,
				this.targetRegistryLocation = targetRegistryLocation);
	}
	
	@Override
	public String getTargetRegistryUsername() {
		return this.targetRegistryUsername;
	}
	
	@Override
	public void setTargetRegistryUsername(final String targetRegistryUsername) {
		firePropertyChange(PROPERTY_TARGET_REGISTRY_USERNAME, this.targetRegistryUsername,
				this.targetRegistryUsername = targetRegistryUsername);
	}
	
	@Override
	public String getTargetRegistryPassword() {
		return this.targetRegistryPassword;
	}
	
	@Override
	public void setTargetRegistryPassword(final String targetRegistryPassword) {
		firePropertyChange(PROPERTY_TARGET_REGISTRY_PASSWORD, this.targetRegistryPassword,
				this.targetRegistryPassword = targetRegistryPassword);
	}
	
	@Override
	public boolean initializeContainerInfo() {
		this.imageMeta = lookupImageMetadata();
		if (this.imageMeta == null) {
			return false;
		}
		final List<EnvironmentVariable> envVars = this.imageMeta.env().stream().filter(env -> env.indexOf('=') != -1)
				.map(env -> env.split("="))
				.map(splittedEnv -> new EnvironmentVariable(splittedEnv[0], splittedEnv.length > 1 ? splittedEnv[1] : StringUtils.EMPTY))
				.collect(Collectors.toList());
		setEnvironmentVariables(envVars);
		final List<IPort> portSpecs = this.imageMeta.exposedPorts().stream().map(spec -> new PortSpecAdapter(spec))
				.collect(Collectors.toList());
		setPortSpecs(portSpecs);
		if(this.imageMeta.volumes() != null && !this.imageMeta.volumes().isEmpty()) {
			setVolumes(new ArrayList<>(this.imageMeta.volumes()));
		} else {
			setVolumes(new ArrayList<>());
		}
		setReplicas(DEFAULT_REPLICA_COUNT);
		return true;
	}
	
	@Override
	public List<EnvironmentVariable> getEnvironmentVariables() {
		return envModel.getEnvironmentVariables();
	}

	@Override
	public boolean isEnvironmentVariableModified(EnvironmentVariable envVar) {
		return envModel.isEnvironmentVariableModified(envVar);
	}
	
	@Override
	public void setEnvironmentVariables(List<EnvironmentVariable> envVars) {
		envModel.setEnvironmentVariables(envVars);
	}

	@Override
	public EnvironmentVariable getEnvironmentVariable(String key) {
		return envModel.getEnvironmentVariable(key);
	}

	@Override
	public void setVolumes(List<String> volumes) {
		firePropertyChange(PROPERTY_VOLUMES, 
				this.volumes, 
				this.volumes = volumes);
	}

	@Override
	public List<String> getVolumes() {
		return volumes;
	}
	
	private void setPortSpecs(List<IPort> portSpecs) {
		firePropertyChange(PROPERTY_PORT_SPECS, 
				this.portSpecs, 
				this.portSpecs = portSpecs);
		setServicePortsFromPorts(portSpecs);
	}
	
	private void setServicePortsFromPorts(List<IPort> portSpecs) {
		this.imagePorts = new ArrayList<>(portSpecs.size());
		List<IServicePort> servicePorts = new ArrayList<>(portSpecs.size());
		for (IPort port : portSpecs) {
			servicePorts.add(new ServicePortAdapter(port));
			imagePorts.add(new ServicePortAdapter(port));
		}
		setServicePorts(servicePorts);
	}
	
	private void setServicePorts(List<IServicePort> servicePorts) {
		firePropertyChange(IServiceAndRoutingPageModel.PROPERTY_SERVICE_PORTS, 
				this.servicePorts, 
				this.servicePorts = servicePorts);
	}

	@Override
	public List<IPort> getPortSpecs() {
		return portSpecs;
	}

	@Override
	public int getReplicas() {
		return replicas;
	}

	@Override
	public void setReplicas(int replicas) {
		firePropertyChange(PROPERTY_REPLICAS, 
				this.replicas, 
				this.replicas = replicas);
	}

	@Override
	public boolean hasConnection() {
		return this.connection != null;
	}

	@Override
	public Object getContext() {
		return null;
	}

	@Override
	public boolean isAddRoute() {
		return addRoute;
	}

	@Override
	public void setAddRoute(boolean addRoute) {
		firePropertyChange(PROPERTY_ADD_ROUTE, 
				this.addRoute, 
				this.addRoute = addRoute);
	}

	@Override
	public List<IServicePort> getServicePorts() {
		return servicePorts;
	}

	@Override
	public void setSelectedEnvironmentVariable(EnvironmentVariable envVar) {
		envModel.setSelectedEnvironmentVariable(envVar);
	}

	@Override
	public EnvironmentVariable getSelectedEnvironmentVariable() {
		return envModel.getSelectedEnvironmentVariable();
	}

	@Override
	public void removeEnvironmentVariable(EnvironmentVariable envVar) {
		envModel.removeEnvironmentVariable(envVar);
	}

	@Override
	public void updateEnvironmentVariable(EnvironmentVariable envVar, String key, String value) {
		envModel.updateEnvironmentVariable(envVar, key, value);
	}	

	@Override
	public void resetEnvironmentVariable(EnvironmentVariable envVar) {
		envModel.resetEnvironmentVariable(envVar);
	}


	@Override
	public void addEnvironmentVariable(String key, String value) {
		envModel.addEnvironmentVariable(key, value);
	}
	
	

	@Override
	public void addServicePort(IServicePort port) {
		if(this.servicePorts.contains(port)) {
			return;
		}
		List<IServicePort> old = new ArrayList<>(this.servicePorts);
		this.servicePorts.add(port);
		firePropertyChange(PROPERTY_SERVICE_PORTS, old, Collections.unmodifiableList(servicePorts));
	}
	
	@Override
	public void updateServicePort(IServicePort source, IServicePort target){
		final int pos = this.servicePorts.indexOf(source);
		if(pos > -1) {
			List<IServicePort> old = new ArrayList<>(this.servicePorts);
			this.servicePorts.set(pos, target);
			fireIndexedPropertyChange(PROPERTY_SERVICE_PORTS, pos, old, Collections.unmodifiableList(servicePorts));
		}
	}

	@Override
	public void setSelectedVolume(String volume) {
		firePropertyChange(PROPERTY_SELECTED_VOLUME, 
				this.selectedVolume, 
				this.selectedVolume = volume);
	}

	@Override
	public String getSelectedVolume() {
		return selectedVolume;
	}

	@Override
	public void updateVolume(String volume, String value) {
		Set<String> old = new LinkedHashSet<>(volumes);
		this.volumes.remove(volume);
		this.volumes.add(value);
		firePropertyChange(PROPERTY_VOLUMES, old, Collections.unmodifiableList(volumes));
	}

	@Override
	public void setSelectedServicePort(IServicePort servicePort) {
		firePropertyChange(PROPERTY_SELECTED_SERVICE_PORT, 
				this.selectedServicePort, 
				this.selectedServicePort = servicePort);
	}

	@Override
	public IServicePort getSelectedServicePort() {
		return selectedServicePort;
	}

	@Override
	public void removeServicePort(IServicePort port) {
		int index = servicePorts.indexOf(port);
		if(index > -1) {
			List<IServicePort> old = new ArrayList<>(servicePorts);
			this.servicePorts.remove(port);
			fireIndexedPropertyChange(PROPERTY_SERVICE_PORTS, index, old, Collections.unmodifiableList(servicePorts));
		}
	}

	@Override
	public void setDockerConnection(IDockerConnection dockerConnection) {
		firePropertyChange(PROPERTY_DOCKER_CONNECTION, this.dockerConnection, this.dockerConnection = dockerConnection);
		this.imageNames.clear();
		if(dockerConnection == null) {
			return;
		}
		final List<IDockerImage> images = dockerConnection.getImages();
		if(images != null) {
			this.imageNames.addAll(dockerConnection.getImages().stream()
					.filter(image -> !image.isDangling() && !image.isIntermediateImage())
					.flatMap(image -> image.repoTags().stream()).sorted().collect(Collectors.toList()));
		}
	}

	
	
	protected IDockerImageMetadata lookupImageMetadata() {
		if (StringUtils.isBlank(this.imageName)) {
			return null;
		}
		final DockerImageURI imageURI = new DockerImageURI(this.imageName);
		final String repo = imageURI.getUriWithoutTag();
		final String tag = StringUtils.defaultIfBlank(imageURI.getTag(), "latest");
		
		if (dockerConnection != null && DockerImageUtils.hasImage(dockerConnection, repo, tag)) {
			final IDockerImageInfo info = dockerConnection.getImageInfo(this.imageName);
			return new DockerConfigMetaData(info);
		} else if (this.project != null) {
			return OpenShiftProjectUtils.lookupImageMetadata(project, imageURI);
		}
		return null;
	}
	
	@Override
	public List<String> getImageNames() {
		return this.imageNames;
	}

	@Override
	public void resetServicePorts() {
		List<IServicePort> ports = imagePorts.stream().map(sp -> new ServicePortAdapter(sp)).collect(Collectors.toList());
		setServicePorts(ports);
	}

	@Override
	public Map<String, String> getImageEnvVars() {
		return envModel.getImageEnvVars();
	}
	
	private static class DockerConfigMetaData implements IDockerImageMetadata {

		private IDockerImageInfo info;

		public DockerConfigMetaData(IDockerImageInfo info) {
			this.info = info;
		}

		@Override
		public Set<String> exposedPorts() {
			return info.containerConfig().exposedPorts();
		}

		@Override
		public List<String> env() {
			return info.containerConfig().env();
		}

		@Override
		public Set<String> volumes() {
			return info.containerConfig().volumes();
		}
		
	}

    @Override
    public void changeEvent(int event) {
    }
    @Override
    public void changeEvent(IDockerConnection connection, int event) {
        if ((event == ADD_EVENT) || (event == REMOVE_EVENT)) {
            firePropertyChange(PROPERTY_DOCKER_CONNECTIONS, dockerConnections, dockerConnections = Arrays.asList(DockerConnectionManager.getInstance().getConnections()));
        }
    }
}
