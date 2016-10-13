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
package org.jboss.tools.openshift.internal.ui.job;

import static org.jboss.tools.openshift.internal.ui.job.ResourceCreationJobUtils.createErrorStatusForExistingResources;
import static org.jboss.tools.openshift.internal.ui.job.ResourceCreationJobUtils.findExistingResources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.core.util.OpenShiftProjectUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dockerutils.DockerImageUtils;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel.Label;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.NotFoundException;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.http.IHttpConstants;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
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
 * @author Jeff Maury
 *
 */
public class DeployImageJob extends AbstractDelegatingMonitorJob 
	implements IResourcesModel, ICommonAttributes{
	
	public static final String SELECTOR_KEY = "deploymentconfig";
	private static final String JBOSSTOOLS_OPENSHIFT = "jbosstools-openshift";
	private static final String MSG_NO_IMAGESTREAM = "{0} Note: Could not find an image stream\nfor {1} and/or the image is not available to the cluster.\nMake sure that a Docker image with that tag is available on the node for the\ndeployment to succeed.";
	
	private IDeployImageParameters parameters;
	private Collection<IResource> created = Collections.emptyList();
	private String summaryMessage;
	
	public DeployImageJob(IDeployImageParameters parameters) {
		this("Deploy Image Job", parameters);
	}

	protected DeployImageJob(String title, IDeployImageParameters parameters) {
		super(title);
		this.parameters = parameters;
		this.summaryMessage = NLS.bind("Results of deploying image \"{0}\".",  parameters.getResourceName());
	}
	
	protected IDeployImageParameters getParameters() {
		return this.parameters;
	}
	
	public String getSummaryMessage() {
		return this.summaryMessage;
	}
	
	@Override
	public Collection<IResource> getResources(){
		return created;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			final Connection connection = parameters.getConnection();
			final String name = parameters.getResourceName();
			if(updateTriggerIfUpdate(connection, parameters.getProject().getName(), name)) {
				return Status.OK_STATUS;
			}
			Map<String, IResource> resources = generateResources(connection, name);
			
			//validate 
			Collection<IResource> existing = findExistingResources(connection, resources.values());
			//TODO may need to disregard if only error is the imagestream - TBD
			if(!existing.isEmpty()) {
				return createErrorStatusForExistingResources(existing);
			}
			//create
			created = createResources(connection, resources.values());
		} catch(Exception e) {
			String message = NLS.bind("Unable to create resources to deploy image {0}", parameters.getImageName());
			OpenShiftUIActivator.getDefault().getLogger().logError(message, e);
			return new Status(IStatus.ERROR, 
					OpenShiftUIActivator.PLUGIN_ID, 
					message,
					e);
		}
		return Status.OK_STATUS;
	}

	protected boolean updateTriggerIfUpdate(Connection connection, String project, String name) {
		try {
			IDeploymentConfig dc = connection.getResource(ResourceKind.DEPLOYMENT_CONFIG, project, name);
			IDeploymentImageChangeTrigger trigger = (IDeploymentImageChangeTrigger) dc.getTriggers().stream()
					.filter(t->DeploymentTriggerType.IMAGE_CHANGE.equals(t.getType())).findFirst().orElse(null);
			if (trigger == null || 
				!ResourceKind.IMAGE_STREAM_TAG.equals(trigger.getKind()) || 
				StringUtils.isBlank(trigger.getNamespace()) ||  
				connection.getResource(ResourceKind.IMAGE_STREAM, trigger.getNamespace(), trigger.getFrom().getName()) == null) {
				return false;
			};
			DockerImageURI sourceImage = getSourceImage();
			if (sourceImage.getName().equals(trigger.getFrom().getName()) &&
					!sourceImage.getTag().equals(trigger.getFrom().getTag())) {
				trigger.setFrom(new DockerImageURI(null, null, sourceImage.getName(), sourceImage.getTag()));
				connection.updateResource(dc);
			}
			return true;
		} catch(NotFoundException e) {
			return false;
		} catch(OpenShiftException e) {
			if(e.getStatus() != null && e.getStatus().getCode() == IHttpConstants.STATUS_NOT_FOUND) {
				return false;
			}
			throw e;
		}
	}

	private Collection<IResource> createResources(Connection connection, Collection<IResource> resources) {
		Collection<IResource> created = new ArrayList<>();
		for (IResource resource : resources) {
			Trace.debug("Trying to create resource: {0}", resource.toJson());
			try {
				created.add(connection.createResource(resource));
			} catch(OpenShiftException e) {
				if(e.getStatus() != null) {
					created.add(e.getStatus());
					OpenShiftUIActivator.getDefault().getLogger().logError(NLS.bind("Error creating resource: {0}", e.getStatus().toJson()));
				} else {
					throw e;
				}
			}
		}
		return created;
	}

	private Map<String, IResource> generateResources(final Connection connection, final String name) {
		final IResourceFactory factory = connection.getResourceFactory();
		final IProject project = parameters.getProject();
		DockerImageURI sourceImage = getSourceImage();
		
		Map<String, IResource> resources = new HashMap<>(4);

		IImageStream is = stubImageStream(factory, name, project, sourceImage);
		if(is != null && StringUtils.isBlank(is.getResourceVersion())) {
			resources.put(ResourceKind.IMAGE_STREAM, is);
		}
		
		resources.put(ResourceKind.SERVICE, stubService(factory, name, SELECTOR_KEY, name));
		
		if(parameters.isAddRoute()) {
			resources.put(ResourceKind.ROUTE, stubRoute(factory, name, resources.get(ResourceKind.SERVICE).getName()));
		}
		
		resources.put(ResourceKind.DEPLOYMENT_CONFIG, stubDeploymentConfig(factory, name, sourceImage, is));
		addToGeneratedResources(resources, connection, name, project);
		
		for (IResource resource : resources.values()) {
			addLabelsToResource(resource);
			resource.setAnnotation(OpenShiftAPIAnnotations.GENERATED_BY, JBOSSTOOLS_OPENSHIFT);
		}
		return resources;
	}
	
	protected void addToGeneratedResources(Map<String, IResource> resources, final Connection connection, final String name, final IProject project) {
		
	}
	
	protected DockerImageURI getSourceImage() {
		String imageName;
		if (parameters.isPushImageToRegistry()) {
			imageName = parameters.getProject().getName() +"/" +  DockerImageUtils.extractImageNameAndTag(parameters.getImageName());
		} else {
			imageName = parameters.getImageName();
		}
		return new DockerImageURI(imageName);
	}
	
	protected IResource stubDeploymentConfig(IResourceFactory factory, final String name, DockerImageURI imageUri, IImageStream is) {
		IDeploymentConfig dc = factory.stub(ResourceKind.DEPLOYMENT_CONFIG, name, parameters.getProject().getName());
		dc.addLabel(SELECTOR_KEY, name);
		dc.addTemplateLabel(SELECTOR_KEY, name);
		for (Label label : parameters.getLabels()) {
			dc.addTemplateLabel(label.getName(), label.getValue());
		}
		dc.setReplicas(parameters.getReplicas());
		dc.setReplicaSelector(SELECTOR_KEY, name);
		
		Map<String, String> envs = getModifiedEnvVars(parameters.getEnvironmentVariables(), parameters.getImageEnvVars());
		dc.addContainer(dc.getName(), imageUri, new HashSet<>(parameters.getPortSpecs()), envs, parameters.getVolumes());
		
		dc.addTrigger(DeploymentTriggerType.CONFIG_CHANGE);
		if(is != null) {
			IDeploymentImageChangeTrigger imageChangeTrigger = (IDeploymentImageChangeTrigger) dc.addTrigger(DeploymentTriggerType.IMAGE_CHANGE);
			imageChangeTrigger.setAutomatic(true);
			imageChangeTrigger.setContainerName(name);
			imageChangeTrigger.setFrom(new DockerImageURI(null, null, is.getName(), imageUri.getTag()));
			imageChangeTrigger.setKind(ResourceKind.IMAGE_STREAM_TAG);
			imageChangeTrigger.setNamespace(is.getNamespace());
		}
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

	protected IImageStream stubImageStream(IResourceFactory factory, String name, IProject project, DockerImageURI imageUri) {
		//get project is - check
		IImageStream is = findImageStreamFor(project.getName(), imageUri);
		if(is == null) {
			//get openshift is - check
		    if (StringUtils.isNotBlank(getParameters().getConnection().getClusterNamespace())) {
	            is = findImageStreamFor((String) getParameters().getConnection().getClusterNamespace(), imageUri);
		    }
			
			//check if cluster will be able to pull image
			if(is == null && isImageVisibleByOpenShift(project, imageUri)){
				is = factory.stub(ResourceKind.IMAGE_STREAM, name, project.getName());
				is.setDockerImageRepository(imageUri.getUriWithoutTag());
			}
		}
		if(is == null) {
			summaryMessage = NLS.bind(MSG_NO_IMAGESTREAM, summaryMessage, parameters.getImageName());
		}
		return is;
	}
	
	private IImageStream findImageStreamFor(String namespace, DockerImageURI uri){
		Connection connection = parameters.getConnection();
		try {
            List<IImageStream> streams = connection.getResources(ResourceKind.IMAGE_STREAM, namespace);
            return streams.stream()
            	.filter(is->is.getDockerImageRepository() != null && is.getDockerImageRepository().getUriUserNameAndName().equals(uri.getUriUserNameAndName()))
            	.findFirst().orElse(null);
        } catch (OpenShiftException e) {
            OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
            return null;
        }
	}
	
	
	/**
	 * Determine if the image is visible by the cluser.  Will use
	 * to create an imagestream ref it.
	 * @param uri
	 * @return
	 */
	protected boolean isImageVisibleByOpenShift(IProject project, DockerImageURI uri) {
		return OpenShiftProjectUtils.lookupImageMetadata(project, uri) != null;
	}
	

	private IResource stubRoute(IResourceFactory factory, String name, String serviceName) {
		IRoute route = factory.stub(ResourceKind.ROUTE, name, parameters.getProject().getName());
		route.setServiceName(serviceName);
		String hostname = parameters.getRouteHostname();
		if (StringUtils.isNotBlank(hostname)) {
		    route.setHost(hostname);
		}
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
