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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;
import org.jboss.tools.openshift.internal.core.IDockerImageMetadata;
import org.jboss.tools.openshift.internal.core.models.PortSpecAdapter;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IServicePort;
import com.openshift.restclient.model.image.IImageStreamImport;

/**
 * The Wizard model to support deploying an image to OpenShift
 * @author jeff.cantrill
 *
 */
public class DeployImageWizardModel 
		extends ResourceLabelsPageModel 
		implements IDeployImageParameters{

	private static final int DEFAULT_REPLICA_COUNT = 1;

	private Connection connection;
	private IProject project;
	private String resourceName;
	private String imageName;
	private Collection<IProject> projects = Collections.emptyList();

	private List<EnvironmentVariable> environmentVariables = Collections.emptyList();
	private Map<String, String> imageEnvVars = new HashMap<>();
	private EnvironmentVariable selectedEnvironmentVariable = null;

	private List<String> volumes = Collections.emptyList();
	private String selectedVolume;

	private List<IPort> portSpecs = Collections.emptyList();

	private int replicas;

	private boolean addRoute = false;

	List<IServicePort> servicePorts = new ArrayList<>();
	IServicePort selectedServicePort = null;
	
	private IDockerConnection dockerConnection;
	private ArrayList<IServicePort> imagePorts;
	private boolean originatedFromDockerExplorer;
	private IDockerImageMetadata imageMeta;
	
	private final List<String> imageNames = new ArrayList<>();
	
	@Override
	public void setOriginatedFromDockerExplorer(boolean orig) {
		this.originatedFromDockerExplorer = orig;
	}
	
	@Override
	public boolean originatedFromDockerExplorer() {
		return originatedFromDockerExplorer;
	}
	
	@Override
	public List<IDockerConnection> getDockerConnections() {
		List<IDockerConnection> all = Arrays.asList(DockerConnectionManager.getInstance().getConnections());
		return all;
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
	public void initModel(final Connection connection, final IProject project) {
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		if(this.connection != null) {
			Job job = new AbstractDelegatingMonitorJob("Loading projects...") {
				
				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						List<IProject> projects = connection.getResources(ResourceKind.PROJECT);
						setProjects(projects);
						return Status.OK_STATUS;
					}catch(Exception e) {
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to load the OpenShift projects for the selected connection.", e);
					}
				}
				
			};
			if(project != null) {
				job.addJobChangeListener(new JobChangeAdapter() {

					@Override
					public void done(IJobChangeEvent event) {
						setProject(project);
					}
				});
			}
			job.schedule();
		}
	}

	@Override
	public void setConnection(final Connection connection) {
		initModel(connection, null);
	}

	@Override
	public void setProjects(Collection<IProject> projects) {
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
	public Collection<IProject> getProjects() {
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
		setResourceName(uri.getName());
	}
	
	@Override
	public boolean initializeContainerInfo() {
		this.imageMeta = lookupImageMetadata();
		if (this.imageMeta == null) {
			return false;
		}
		final List<EnvironmentVariable> envVars = this.imageMeta.env().stream().filter(env -> env.indexOf('=') != -1)
				.map(env -> env.split("=")).map(splittedEnv -> new EnvironmentVariable(splittedEnv[0], splittedEnv[1]))
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
		return environmentVariables;
	}
	
	@Override
	public void setEnvironmentVariables(List<EnvironmentVariable> envVars) {
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, 
				this.environmentVariables, 
				this.environmentVariables = envVars);
		this.imageEnvVars.clear();
		for (IKeyValueItem label : envVars) {
			imageEnvVars.put(label.getKey(), label.getValue());
		}
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
		firePropertyChange(PROPERTY_SELECTED_ENVIRONMENT_VARIABLE, 
				this.selectedEnvironmentVariable, 
				this.selectedEnvironmentVariable = envVar);
	}

	@Override
	public EnvironmentVariable getSelectedEnvironmentVariable() {
		return selectedEnvironmentVariable;
	}

	@Override
	public void removeEnvironmentVariable(EnvironmentVariable envVar) {
		final int i = environmentVariables.indexOf(envVar);
		if(i > -1) {
			List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
			this.environmentVariables.remove(i);
			fireIndexedPropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, i, old, Collections.unmodifiableList(environmentVariables));
		}
	}

	@Override
	public void updateEnvironmentVariable(EnvironmentVariable envVar, String key, String value) {
		final int i = environmentVariables.indexOf(envVar);
		if(i > -1) {
			List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
			EnvironmentVariable prev = environmentVariables.get(i);
			environmentVariables.set(i, new EnvironmentVariable(key, value, prev.isNew()));
			fireIndexedPropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, i, old, Collections.unmodifiableList(environmentVariables));
		}
	}	
	@Override
	public void resetEnvironmentVariable(EnvironmentVariable envVar) {
		if(imageEnvVars.containsKey(envVar.getKey())) {
			updateEnvironmentVariable(envVar, envVar.getKey(), imageEnvVars.get(envVar.getKey()));
		}
	}


	@Override
	public void addEnvironmentVariable(String key, String value) {
		List<EnvironmentVariable> old = new ArrayList<>(environmentVariables);
		this.environmentVariables.add(new EnvironmentVariable(key, value, true));
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, old, Collections.unmodifiableList(environmentVariables));
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
		this.imageNames.addAll(dockerConnection.getImages().stream()
				.filter(image -> !image.isDangling() && !image.isIntermediateImage())
				.flatMap(image -> image.repoTags().stream()).sorted().collect(Collectors.toList()));
	}

	
	
	private IDockerImageMetadata lookupImageMetadata() {
		if (dockerConnection == null || StringUtils.isBlank(this.imageName)) {
			return null;
		}
		final DockerImageURI imageURI = new DockerImageURI(this.imageName);
		final String repo = imageURI.getUriWithoutTag();
		final String tag = StringUtils.defaultIfBlank(imageURI.getTag(), "latest");
		if (dockerConnection.hasImage(repo, tag)) {
			final IDockerImageInfo info = dockerConnection.getImageInfo(this.imageName);
			return new DockerConfigMetaData(info);
		} else if (this.project != null && project.supports(IImageStreamImportCapability.class)) {
			final IImageStreamImportCapability cap = project.getCapability(IImageStreamImportCapability.class);
			try {
				final IImageStreamImport streamImport = cap.importImageMetadata(imageURI);
				if (ResourceUtils.isSuccessful(streamImport)) {
					return new ImportImageMetaData(streamImport.getImageJsonFor(imageURI));
				}
			} catch (OpenShiftException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
			}
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
		return Collections.unmodifiableMap(this.imageEnvVars);
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

	private static class ImportImageMetaData implements IDockerImageMetadata {

		private static final String[] ROOT = new String [] {"image","dockerImageMetadata","ContainerConfig"};
		private static final String[] PORTS = (String [])ArrayUtils.add(ROOT, "ExposedPorts");
		private static final String[] ENV = (String [])ArrayUtils.add(ROOT, "Env");
		private static final String[] VOLUMES = (String [])ArrayUtils.add(ROOT, "Volumes");
		private final ModelNode node;

		public ImportImageMetaData(final String json) {
			this.node = ModelNode.fromJSONString(json);
		}

		@Override
		public Set<String> exposedPorts(){
			ModelNode ports = node.get(PORTS);
			if(ports.isDefined()) {
				return ports.keys();
			}
			return Collections.emptySet();
		}
		
		@Override
		public List<String> env(){
			ModelNode env = node.get(ENV);
			if(env.isDefined()) {
				return env.asList().stream().map(n->n.asString()).collect(Collectors.toList());
			}
			return Collections.emptyList();
		}
		
		@Override
		public Set<String> volumes(){
			ModelNode volumes = node.get(VOLUMES);
			if(volumes.isDefined()) {
				return volumes.keys();
			}
			return Collections.emptySet();
		}
	}
}
