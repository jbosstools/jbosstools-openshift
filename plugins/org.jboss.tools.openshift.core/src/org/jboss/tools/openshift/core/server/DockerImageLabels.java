/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc.. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;
import com.openshift.restclient.model.image.IImageStreamImport;

public class DockerImageLabels {

	//	private static final String DOCKER_IMAGE_DIGEST_IDENTIFIER = "sha256:";
	private static final String SHARED_DATA_KEY = "DOCKER_IMAGE_LABELS";

	private IResource resource;
	private Connection connection;
	private String metadata;
	private DevmodeMetadata devmodeMetadata;
	private PodDeploymentPathMetadata podPathMetadata;

	/**
	 * Returns an instance for a given server behaviour and resource. The server
	 * behaviour shared data is looked up for a matching instance. If it doesn't
	 * exists a new one is created. The docker image metadata is lazyly loaded when
	 * data is requested via {@link #getPodPath()}, {@link #getDevmodePortKey()}
	 * etc.
	 * 
	 * @param resource
	 * @param behaviour
	 * @return
	 * 
	 * @see IControllableServerBehavior
	 * @see IResource
	 */
	public static DockerImageLabels getInstance(IResource resource, IControllableServerBehavior behaviour) {
		DockerImageLabels metadata = (DockerImageLabels) behaviour.getSharedData(SHARED_DATA_KEY);
		if (metadata == null || !Objects.equals(resource, metadata.resource)) {
			Connection connection = OpenShiftServerUtils.getConnection(behaviour.getServer());
			metadata = new DockerImageLabels(resource, connection);
			behaviour.putSharedData(SHARED_DATA_KEY, metadata);
		}
		return metadata;
	}

	protected DockerImageLabels(IResource resource, Connection connection) {
		this.resource = resource;
		this.connection = connection;
	}

	public String getDevmodeKey() {
		if (!loadIfRequired()) {
			return null;
		}
		return devmodeMetadata.getEnablementKey();
	}

	public String getDevmodePortKey() {
		if (!loadIfRequired()) {
			return null;
		}

		return devmodeMetadata.getPortKey();
	}

	public String getDevmodePortValue() {
		if (!loadIfRequired()) {
			return null;
		}
		return devmodeMetadata.getPortValue();
	}

	public String getPodPath() {
		if (!loadIfRequired()) {
			return null;
		}
		return this.podPathMetadata.get();
	}

	public boolean load() {
		return loadIfRequired();
	}

	private boolean isLoaded() {
		return metadata != null;
	}

	protected boolean loadIfRequired() {
		if (isLoaded()) {
			return true;
		}
		this.metadata = load(resource);
		if (StringUtils.isEmpty(metadata)) {
			return false;
		}
		this.devmodeMetadata = new DevmodeMetadata(metadata);
		this.podPathMetadata = new PodDeploymentPathMetadata(metadata);
		return true;
	}

	public boolean isAvailable() {
		return isLoaded();
	}

	/**
	 * Loads the docker image meta data for a given resource. The given resource is
	 * used to infer a deployment config which then is used to determined the docker
	 * image being used. The meta data of this docker image is then loaded.
	 * 
	 * @param reosurce
	 *            the openshift resource to load the image metadata for
	 * @return
	 */
	protected String load(IResource resource) {
		IDeploymentConfig dc = ResourceUtils.getDeploymentConfigFor(resource, connection);
		if (dc == null) {
			return null;
		}

		IDeploymentImageChangeTrigger trigger = getImageChangeTrigger(dc.getTriggers());
		if (trigger == null) {
			return null;
		}
		DockerImageURI uri = trigger.getFrom();
		return getImageStreamTag(uri, resource.getNamespaceName());
		//		String imageRef = getImageRef(dc, connection);
		//		int imageDigestIndex = imageRef.indexOf(DOCKER_IMAGE_DIGEST_IDENTIFIER);
		//		if (imageDigestIndex > 0) {
		//			String imageDigest = imageRef.substring(imageDigestIndex);
		//			return getImageStreamTag(imageDigest, imageRef, project.getName(), connection);
		//		} else {
		//			IImageStream imageStream = connection.getResource(ResourceKind.IMAGE_STREAM, project.getName(), imageRef);
		//			if (	imageStream != null) {
		////				DockerImageURI uri = imageStream.getDockerImageRepository();
		////				return importImageStream(uri.getAbsoluteUri(), project);
		//				IDockerImageMetadata metadata = DockerImageUtils.lookupImageMetadata(project, uri);
		//				return metadata.toString();
		//			} else {
		//				return importImageStream(imageRef, project);
		//			}
		//		}
	}

	private IDeploymentImageChangeTrigger getImageChangeTrigger(Collection<IDeploymentTrigger> triggers) {
		for (IDeploymentTrigger trigger : triggers) {
			if (DeploymentTriggerType.IMAGE_CHANGE.equals(trigger.getType())) {
				return (IDeploymentImageChangeTrigger) trigger;
			}
		}
		return null;
	}

	private String getImageStreamTag(DockerImageURI uri, String namespace) {
		try {
			IResource imageStreamTag = connection.getResource(ResourceKind.IMAGE_STREAM_TAG, namespace,
					uri.getAbsoluteUri());
			return imageStreamTag.toJson();
		} catch (OpenShiftException e) {
			return null;
		}
	}

	private String getImageRef(IDeploymentConfig dc, Connection connection) throws CoreException {
		Collection<String> images = dc.getImages();
		if (images.isEmpty()) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory()
					.errorStatus(NLS.bind("No images found for deployment config {0} in project {1} on server {2}",
							new Object[] { dc.getName(), dc.getNamespaceName(), connection.getHost() })));
		}
		// TODO: handle if there are 2+ images
		return images.iterator().next();
	}

	private String getImageStreamTag(String imageDigest, String imageRef, String namespace, Connection connection) {
		List<IResource> imageStreamTags = connection.getResources(ResourceKind.IMAGE_STREAM_TAG, namespace);
		IResource imageStreamTag = ResourceUtils.getImageStreamTagForDigest(imageDigest, imageStreamTags);
		if (imageStreamTag == null) {
			return null;
		}
		// load image stream tag individually to get full ContainerConfig metadata
		imageStreamTag = connection.getResource(ResourceKind.IMAGE_STREAM_TAG, namespace, imageStreamTag.getName());
		return imageStreamTag.toJson();
	}

	private String importImageStream(String imageRef, IProject project) {
		IImageStreamImportCapability imageStreamImportCapability = project
				.getCapability(IImageStreamImportCapability.class);
		DockerImageURI uri = new DockerImageURI(imageRef);
		IImageStreamImport imageStreamImport = imageStreamImportCapability.importImageMetadata(uri);
		if (!ResourceUtils.isSuccessful(imageStreamImport)) {
			return null;
		}
		return imageStreamImport.getImageJsonFor(uri);
	}
}
