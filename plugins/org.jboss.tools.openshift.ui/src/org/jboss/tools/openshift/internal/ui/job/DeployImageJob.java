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
package org.jboss.tools.openshift.internal.ui.job;

import static org.jboss.tools.openshift.internal.ui.job.ResourceCreationJobUtils.createErrorStatusForExistingResources;
import static org.jboss.tools.openshift.internal.ui.job.ResourceCreationJobUtils.findExistingResources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel.Label;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.route.IRoute;

/**
 * Job to deploy docker images to OpenShift with a minimal
 * set of OpenShift resources
 * 
 * @author jeff.cantrill
 *
 */
public class DeployImageJob extends AbstractDelegatingMonitorJob implements IResourcesModel{
	
	public static final String SELECTOR_KEY = "deploymentconfig";
	
	private IDeployImageParameters parameters;
	private Collection<IResource> created = Collections.emptyList();
	
	public DeployImageJob(IDeployImageParameters parameters) {
		super("Deploy Image Job");
		this.parameters = parameters;
	}
	
	public Collection<IResource> getResources(){
		return created;
	}
	
	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			final Connection connection = parameters.getConnection();
			final IResourceFactory factory = connection.getResourceFactory();
			final String name = parameters.getResourceName();
			Map<String, IResource> resources = generateResources(factory, name);
			
			//validate 
			Collection<IResource> existing = findExistingResources(connection, resources.values());
			//TODO may need to disregard if only error is the imagestream - TBD
			if(!existing.isEmpty()) {
				return createErrorStatusForExistingResources(existing);
			}
			//create
			created = createResources(connection, resources.values());
		}catch(Exception e) {
			return new Status(IStatus.ERROR, 
					OpenShiftUIActivator.PLUGIN_ID, 
					NLS.bind("Unable to create resources to deploy image {0}", parameters.getImageName()),
					e);
		}
		return Status.OK_STATUS;
	}

	private Collection<IResource>  createResources(Connection connection, Collection<IResource> resources) {
		Collection<IResource> created = new ArrayList<>();
		for (IResource resource : resources) {
			Trace.debug("Trying to create resource: {0}", resource.toJson());
			try {
				created.add(connection.createResource(resource));
			}catch(OpenShiftException e) {
				if(e.getStatus() != null) {
					created.add(e.getStatus());
					OpenShiftUIActivator.getDefault().getLogger().logError(NLS.bind("Error creating resource: {0}", e.getStatus().toJson()));
				}else {
					throw e;
				}
			}
		}
		return created;
	}

	private Map<String, IResource> generateResources(final IResourceFactory factory, final String name) {
		final IProject project = parameters.getProject();
		DockerImageURI sourceImage = new DockerImageURI(parameters.getImageName());
		
		Map<String, IResource> resources = new HashMap<String, IResource>(4);

		resources.put(ResourceKind.IMAGE_STREAM, stubImageStream(factory, name, project, sourceImage));
		
		resources.put(ResourceKind.SERVICE, stubService(factory, name, SELECTOR_KEY, name));
		
		if(parameters.isAddRoute()) {
			resources.put(ResourceKind.ROUTE, stubRoute(factory, name, resources.get(ResourceKind.SERVICE).getName()));
		}
		
		resources.put(ResourceKind.DEPLOYMENT_CONFIG, stubDeploymentConfig(factory, name, sourceImage));
		
		for (IResource resource : resources.values()) {
			addLabelsToResource(resource);
			resource.setAnnotation(OpenShiftAPIAnnotations.GENERATED_BY, "jbosstools-openshift");
		}
		return resources;
	}
	
	public IResource stubDeploymentConfig(IResourceFactory factory, final String name, DockerImageURI imageUri) {
		IDeploymentConfig dc = factory.stub(ResourceKind.DEPLOYMENT_CONFIG, name, parameters.getProject().getName());
		dc.addLabel(SELECTOR_KEY, name);
		dc.addTemplateLabel(SELECTOR_KEY, name);
		for (Label label : parameters.getLabels()) {
			dc.addTemplateLabel(label.getName(), label.getValue());
		}
		dc.setReplicas(parameters.getReplicas());
		dc.setReplicaSelector(SELECTOR_KEY, name);
		
		Map<String, String> envs = getModifiedEnvVars(parameters.getEnvironmentVariables(), parameters.getImageEnvVars());
		dc.addContainer(dc.getName(), imageUri, new HashSet<IPort>(parameters.getPortSpecs()), envs, parameters.getVolumes());
		
		dc.addTrigger(DeploymentTriggerType.CONFIG_CHANGE);
		IDeploymentImageChangeTrigger imageChangeTrigger = (IDeploymentImageChangeTrigger) dc.addTrigger(DeploymentTriggerType.IMAGE_CHANGE);
		imageChangeTrigger.setAutomatic(true);
		imageChangeTrigger.setContainerName(name);
		imageChangeTrigger.setFrom(new DockerImageURI(null, null, name, imageUri.getTag()));
		imageChangeTrigger.setKind(ResourceKind.IMAGE_STREAM_TAG);
		return dc;
	}
	
	private Map<String, String> getModifiedEnvVars(Collection<EnvironmentVariable> envVars, Map<String, String> dockerEnvVars){
		Map<String, String> envs = new HashMap<>();
		for (EnvironmentVariable var : parameters.getEnvironmentVariables()) {
			//will return null if new
			if(!StringUtils.defaultIfEmpty(dockerEnvVars.get(var.getKey()),"").equals(var.getValue())){
				envs.put(var.getKey(), var.getValue());
			}
		}
		return envs;
	}

	private IImageStream stubImageStream(IResourceFactory factory, String name, IProject project, DockerImageURI imageUri) {
		IImageStream imageStream = factory.stub(ResourceKind.IMAGE_STREAM, name, parameters.getProject().getName());
		imageStream.setDockerImageRepository(imageUri.getUriWithoutTag());
		return imageStream;
	}

	private IResource stubRoute(IResourceFactory factory, String name, String serviceName) {
		IRoute route = factory.stub(ResourceKind.ROUTE, name, parameters.getProject().getName());
		route.setServiceName(serviceName);
		return route;
	}

	private IService stubService(IResourceFactory factory, String name, String selectorKey, String selectorValue) {
		IService service = factory.stub(ResourceKind.SERVICE, name, parameters.getProject().getName());
		service.setPorts(parameters.getServicePorts());
		service.setSelector(selectorKey, selectorValue);
		return service;
	}
	
	private void addLabelsToResource(IResource resource) {
		for (Label label : parameters.getLabels()) {
			resource.addLabel(label.getName(), label.getValue());
		}
	}

}
