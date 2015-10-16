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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerContainerConfig;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;

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
		implements IDeployImageParameters{

	private static final int DEFAULT_REPLICA_COUNT = 1;
	private Connection connection;
	private IProject project;
	private String name;
	private String image;
	private Collection<IProject> projects = Collections.emptyList();

	private List<EnvironmentVariable> environmentVariables = Collections.emptyList();
	private Map<String, String> imageEnvVars = new HashMap<>();
	private EnvironmentVariable selectedEnvironmentVariable = null;

	private List<String> volumes = Collections.emptyList();
	private String selectedVolume;

	private List<IPort> portSpecs = Collections.emptyList();

	private int replicas;

	private boolean addRoute = false;

	List<IServicePort> servicePorts = new ArrayList<IServicePort>();
	IServicePort selectedServicePort = null;
	
	private IDockerConnection dockerConnection;
	private boolean imageInfoInitialized;
	private ArrayList<IServicePort> imagePorts;
	private boolean originatedFromDockerExplorer;
	
	public DeployImageWizardModel() {
	}
	
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
		if(dockerConnection == null && getDockerConnections().size() == 1) {
			setDockerConnection(getDockerConnections().get(0));
		}
		return dockerConnection;
	}

	@Override
	public Collection<Connection> getConnections() {
		return ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
	}

	@Override
	public Connection getConnection() {
		return connection;
	}
	
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

	private void setProjects(List<IProject> projects) {
		if(projects == null) projects = Collections.emptyList();
		firePropertyChange(PROPERTY_PROJECTS, this.projects, this.projects = projects);
		if(!projects.isEmpty() && !projects.contains(getProject())) {
			setProject(null);
		}
		if(projects.size() == 1) {
			setProject(projects.get(0));
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
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	@Override
	public String getImage() {
		return this.image;
	}

	@Override
	public void setImage(String image) {
		if(StringUtils.isEmpty(image)) return;
		firePropertyChange(PROPERTY_IMAGE, this.image, this.image = image);
		DockerImageURI uri = new DockerImageURI(image);
		setName(uri.getName());
		resetImageInfo();
		initContainerInfo();
	}
	
	private void resetImageInfo() {
		setEnvironmentVariables(new ArrayList<EnvironmentVariable>());
		setVolumes(new ArrayList<String>());
		setPortSpecs(new ArrayList<IPort>());
		setReplicas(DEFAULT_REPLICA_COUNT);
		imageInfoInitialized = false;
	}

	@Override
	public List<EnvironmentVariable> getEnvironmentVariables() {
		return environmentVariables;
	}
	
	private synchronized void initContainerInfo() {
		if(imageInfoInitialized) return;
		IDockerImageInfo info = dockerConnection.getImageInfo(getImage());
		if(info == null) return;
		IDockerContainerConfig imageConfig = info.config();
		setEnvVars(imageConfig.env());
		
		List<IPort> portSpecs = new ArrayList<>();
		Set<String> specs = imageConfig.exposedPorts();
		for (String spec : specs) {
			try {
				portSpecs.add(new PortSpecAdapter(spec));
			}catch(IllegalArgumentException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
			}
		}
		setPortSpecs(portSpecs);
		setVolumes(new ArrayList<String>(imageConfig.volumes()));
		imageInfoInitialized = true;
	}
	
	private void setEnvVars(List<String> env) {
		List<EnvironmentVariable> envVars = new ArrayList<>(env.size());
		for (String var : env) {
			String[] split = var.split("=");
			envVars.add(new EnvironmentVariable(split[0], split[1]));
		}
		setEnvironmentVariables(envVars);
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
	
	public void setPortSpecs(List<IPort> portSpecs) {
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
		// TODO Auto-generated method stub
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
			List<EnvironmentVariable> old = new ArrayList<EnvironmentVariable>(environmentVariables);
			this.environmentVariables.remove(i);
			fireIndexedPropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, i, old, Collections.unmodifiableList(environmentVariables));
		}
	}

	@Override
	public void updateEnvironmentVariable(EnvironmentVariable envVar, String key, String value) {
		final int i = environmentVariables.indexOf(envVar);
		if(i > -1) {
			List<EnvironmentVariable> old = new ArrayList<EnvironmentVariable>(environmentVariables);
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
		List<EnvironmentVariable> old = new ArrayList<EnvironmentVariable>(environmentVariables);
		this.environmentVariables.add(new EnvironmentVariable(key, value, true));
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, old, Collections.unmodifiableList(environmentVariables));
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
		Set<String> old = new LinkedHashSet<String>(volumes);
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
			List<IServicePort> old = new ArrayList<IServicePort>(servicePorts);
			this.servicePorts.remove(port);
			fireIndexedPropertyChange(PROPERTY_SERVICE_PORTS, index, old, Collections.unmodifiableList(servicePorts));
		}
	}
	
	@Override
	public void setDockerConnection(IDockerConnection dockerConnection) {
		firePropertyChange(PROPERTY_DOCKER_CONNECTION, this.dockerConnection, this.dockerConnection = dockerConnection);
	}

	@Override
	public boolean imageExists(String image) {
		return dockerConnection.getImageInfo(image) != null; 
	}

	@Override
	public void resetServicePorts() {
		List<IServicePort> ports = new ArrayList<>(this.imagePorts.size());
		for (IServicePort port : this.imagePorts) {
			ports.add(new ServicePortAdapter(port));
		}
		setServicePorts(ports);
	}

	@Override
	public Map<String, String> getImageEnvVars() {
		return Collections.unmodifiableMap(this.imageEnvVars);
	}
	
	
	
}
