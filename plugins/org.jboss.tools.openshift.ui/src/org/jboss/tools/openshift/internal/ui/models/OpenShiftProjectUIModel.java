/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.util.Collection;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * UI Model for an OpenShift Project
 * @author jeff.cantrill
 *
 */
public class OpenShiftProjectUIModel extends ResourcesUIModel implements IProjectAdapter, IResourceUIModel {
	
	public static final String PROP_LOADING = "loading";
	private final IResourceUIModel resourceModel;

	public OpenShiftProjectUIModel(IProject project) {
		super();
		resourceModel = new OpenShiftResourceUIModel(project);
	}
	
	@Override
	public IProject getProject() {
		return getResource();
	}


	@Override
	public <T extends IResource> T getResource() {
		return resourceModel.getResource();
	}

	public boolean isLoading() {
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResource> void setResources(Collection<T> resources, String kind) {
		switch(kind) {
		case ResourceKind.BUILD:
			setBuildResources((Collection<IBuild>) resources);
			break;
		case ResourceKind.BUILD_CONFIG:
			setBuildConfigResources((Collection<IResource>) resources);
			break;
		case ResourceKind.DEPLOYMENT_CONFIG:
			setDeploymentConfigResources((Collection<IResource>) resources);
			break;
		case ResourceKind.IMAGE_STREAM:
			setImageStreamResources((Collection<IResource>) resources);
			break;
		case ResourceKind.POD:
			setPodResources((Collection<IPod>) resources);
			break;
		case ResourceKind.ROUTE:
			setRouteResources((Collection<IResource>) resources);
			break;
		case ResourceKind.REPLICATION_CONTROLLER:
			setReplicationControllerResources((Collection<IResource>) resources);
			break;
		case ResourceKind.SERVICE:
			setServiceResources((Collection<IResource>) resources);
			break;
		default:
		}

	}
	
}
