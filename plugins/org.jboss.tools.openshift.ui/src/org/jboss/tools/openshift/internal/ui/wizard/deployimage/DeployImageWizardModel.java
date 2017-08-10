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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerConnectionManagerListener;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.docker.DockerConfigMetaData;
import org.jboss.tools.openshift.internal.core.docker.DockerImageUtils;
import org.jboss.tools.openshift.internal.core.docker.IDockerImageMetadata;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
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
 * @author Andre Dietisheim
 *
 */
public class DeployImageWizardModel 
		extends ResourceLabelsPageModel 
		implements IDeployImageParameters, IDockerConnectionManagerListener, PropertyChangeListener {

	private static final int DEFAULT_REPLICA_COUNT = 1;

	private Connection connection;
	private IProject project;
	private List<IProject> projects = new ArrayList<>();
	private String resourceName;
	private String imageName;
	private EnvironmentVariablesPageModel envModel = new EnvironmentVariablesPageModel();
	private List<String> volumes = Collections.emptyList();
	private String selectedVolume;
	private List<IPort> portSpecs = Collections.emptyList();
	private int replicas;
	private boolean addRoute = true;
	private String routeHostname;
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
    private IServicePort routingPort;

	private static final DockerImage2OpenshiftResourceConverter dockerImage2OpenshiftResourceConverter = new DockerImage2OpenshiftResourceConverter();
	private final List<String> imageNames = new ArrayList<>();
    private List<IDockerConnection> dockerConnections = Arrays.asList(DockerConnectionManager.getInstance().getConnections());
    private Comparator<IProject> projectsComparator;
	protected boolean resourcesLoaded = false;

	public DeployImageWizardModel() {
		envModel.addPropertyChangeListener(PROPERTY_ENVIRONMENT_VARIABLES, this);
		envModel.addPropertyChangeListener(PROPERTY_SELECTED_ENVIRONMENT_VARIABLE, this);
		DockerConnectionManager.getInstance().addConnectionManagerListener(this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
	    DockerConnectionManager.getInstance().removeConnectionManagerListener(this);
		((EnvironmentVariablesPageModel) envModel).dispose();
	    this.connection = null;
	    this.project = null;
	    this.dockerConnection = null;
	    this.projects.clear();
	    this.volumes.clear();
	    this.portSpecs.clear();
	    if(imagePorts != null) {
	    	imagePorts.clear();
	    	this.imagePorts = null;
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
		if (dockerConnection == null) {
			List<IDockerConnection> all = getDockerConnections();
			if (all.size() == 1) {
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

	private void initImageRegistry(Connection connection) {
		if(connection == null) {
			return;
		}
		setTargetRegistryLocation(
			(String) connection.getExtendedProperties().get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY));
		setTargetRegistryUsername(connection.getUsername());
		setTargetRegistryPassword(connection.getToken());
	}

	@Override
	public void setConnection(final Connection connection) {
		Connection oldConnection = this.connection;
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		initImageRegistry(connection);
		this.resourcesLoaded = resourcesLoaded && ObjectUtils.equals(oldConnection, connection);
	}

	@Override
	public void loadResources() {
		if (resourcesLoaded) {
			return;
		}

		List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
		setProjects(projects);
		setProjectOrDefault(project);

		this.resourcesLoaded = true;
	}
	
	protected void setProjects(List<IProject> projects) {
		if(projects == null) {
			projects = Collections.emptyList();
		}
		firePropertyChange(PROPERTY_PROJECTS, this.projects, this.projects = projects);
	}

	@Override
	public List<IProject> getProjects() {
		return projects;
	}

	@Override
	public void addProject(IProject project) {
		if (project == null) {
			return;
		}
		List<IProject> newProjects = new ArrayList<>(projects);
		newProjects.add(project);
		setProjects(newProjects);
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
		if(this.imageName != null && !this.imageName.equals(imageName) && this.imageMeta != null) {
			//Clean container info loaded for old image name.
			this.imageMeta = null;
			setEnvironmentVariables(new ArrayList<>());
			setPortSpecs(new ArrayList<>());
			setVolumes(new ArrayList<>());
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
		initEnvVariables();
		initExposedPorts();
		initVolumes();
		setReplicas(DEFAULT_REPLICA_COUNT);
		return true;
	}

	private void initExposedPorts() {
		List<IPort> portSpecs = Collections.emptyList();
		if (imageMeta != null
				&& !CollectionUtils.isEmpty(imageMeta.exposedPorts())) {
			portSpecs = imageMeta.exposedPorts().stream()
					.map(spec -> new PortSpecAdapter(spec))
					.collect(Collectors.toList());
		}
		setPortSpecs(portSpecs);
	}

	private void initEnvVariables() {
		List<EnvironmentVariable> envVars = Collections.emptyList();
		if (imageMeta != null
				&& !CollectionUtils.isEmpty(imageMeta.env())) {
			envVars = imageMeta.env().stream()
					.filter(env -> env != null && env.indexOf('=') != -1)
					.map(env -> env.split("="))
					.map(splittedEnv -> 
						new EnvironmentVariable(splittedEnv[0], splittedEnv.length > 1 ? splittedEnv[1] : StringUtils.EMPTY))
					.collect(Collectors.toList());
		}
		setEnvironmentVariables(envVars);
	}

	private void initVolumes() {
		List<String> volumes = Collections.emptyList();
		if (imageMeta != null
				&& !CollectionUtils.isEmpty(imageMeta.volumes())) {
			volumes = new ArrayList<>(imageMeta.volumes());
		}
		setVolumes(volumes);
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
        if (servicePorts.size() > 0) {
            ServicePortAdapter adapterPort = (ServicePortAdapter) servicePorts.get(0);
            adapterPort.setRoutePort(true);
            setRoutingPort(adapterPort);
        } else {
            setRoutingPort(null);
        }
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
    public String getRouteHostname() {
        return routeHostname;
    }

    @Override
    public void setRouteHostname(String routeHostname) {
        firePropertyChange(PROPERTY_ROUTE_HOSTNAME, this.routeHostname, this.routeHostname = routeHostname);
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
		if (port instanceof ServicePortAdapter && ((ServicePortAdapter)port).isRoutePort()) {
		    setRoutingPort(port);
		}
	}
	
	@Override
	public void updateServicePort(IServicePort source, IServicePort target){
		final int pos = this.servicePorts.indexOf(source);
		if(pos > -1) {
			List<IServicePort> old = new ArrayList<>(this.servicePorts);
			this.servicePorts.set(pos, target);
			/**
			 * databinding would not replace old object with a new one if only a
			 * boolean property is modified. I could not understand why it is
			 * so, but I found that when target port (String) and route port
			 * (Boolean) are changed in Edit dialog together, everything works.
			 * 
			 * @see https://github.com/jbosstools/jbosstools-openshift/pull/1365
			 */
			String p = target.getTargetPort();
			target.setTargetPort("dummy");
			fireIndexedPropertyChange(PROPERTY_SERVICE_PORTS, pos, old, Collections.unmodifiableList(servicePorts));
			if (((ServicePortAdapter)target).isRoutePort()) {
			    setRoutingPort(target);
			} else if (((ServicePortAdapter)source).isRoutePort() && !((ServicePortAdapter)target).isRoutePort()) {
			    setRoutingPort(null);
			}
			target.setTargetPort(p);
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
			if (info == null) {
				return null;
			}
			return new DockerConfigMetaData(info);
		} else if (this.project != null) {
			return DockerImageUtils.lookupImageMetadata(project, imageURI);
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
    public void setRoutingPort(IServicePort port) {
        if (routingPort != null) {
            ((ServicePortAdapter)routingPort).setRoutePort(false);
        }
        firePropertyChange(PROPERTY_ROUTING_PORT, routingPort, this.routingPort = port);
    }

    @Override
    public IServicePort getRoutingPort() {
        return routingPort;
    }

    @Override
	public Map<String, String> getImageEnvVars() {
		return envModel.getImageEnvVars();
	}

    @Override
    public void changeEvent(IDockerConnection connection, int event) {
        if ((event == ADD_EVENT) 
        		|| (event == REMOVE_EVENT)) {
            firePropertyChange(PROPERTY_DOCKER_CONNECTIONS, 
            		dockerConnections, 
            		dockerConnections = Arrays.asList(DockerConnectionManager.getInstance().getConnections()));
        }
    }
}
