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
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;

public class DockerImageLabels {

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

	public String getDevmodeKey(IProgressMonitor monitor) {
		if (!loadIfRequired(monitor)) {
			return null;
		}
		return devmodeMetadata.getEnablementKey();
	}

	public String getDevmodePortKey(IProgressMonitor monitor) {
		if (!loadIfRequired(monitor)) {
			return null;
		}

		return devmodeMetadata.getPortKey();
	}

	public String getDevmodePortValue(IProgressMonitor monitor) {
		if (!loadIfRequired(monitor)) {
			return null;
		}
		return devmodeMetadata.getPortValue();
	}

	public String getPodPath(IProgressMonitor monitor) {
		if (!loadIfRequired(monitor)) {
			return null;
		}
		return this.podPathMetadata.get();
	}

	public boolean load(IProgressMonitor monitor) {
		return loadIfRequired(monitor);
	}

	private boolean isLoaded() {
		return metadata != null;
	}

	protected boolean loadIfRequired(IProgressMonitor monitor) {
		if (isLoaded()) {
			return true;
		}
		this.metadata = load(resource, monitor);
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
	 * @param monitor 
	 * 
	 * @param reosurce
	 *            the openshift resource to load the image metadata for
	 * @return
	 */
	protected String load(IResource resource, IProgressMonitor monitor) {
		IDeploymentConfig dc = ResourceUtils.getDeploymentConfigFor(resource, connection);
		if (dc == null) {
			return null;
		}

		IDeploymentImageChangeTrigger trigger = getImageChangeTrigger(dc.getTriggers());
		if (trigger == null) {
			return null;
		}
		DockerImageURI uri = trigger.getFrom();
		return getImageStreamTag(uri, resource.getNamespaceName(), monitor);
	}

	private IDeploymentImageChangeTrigger getImageChangeTrigger(Collection<IDeploymentTrigger> triggers) {
		if (CollectionUtils.isEmpty(triggers)) {
			return null;
		}

		for (IDeploymentTrigger trigger : triggers) {
			if (DeploymentTriggerType.IMAGE_CHANGE.equals(trigger.getType())) {
				return (IDeploymentImageChangeTrigger) trigger;
			}
		}
		return null;
	}

	private String getImageStreamTag(DockerImageURI uri, String namespace, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind("Loading imagestream tag {0}", uri.getName()), 1);
		try {
			IResource imageStreamTag = connection.getResource(ResourceKind.IMAGE_STREAM_TAG, namespace,
					uri.getAbsoluteUri());
			return imageStreamTag.toJson();
		} catch (OpenShiftException e) {
			subMonitor.done();
			return null;
		}
	}
}
