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
import com.openshift.restclient.model.IResource;

public interface IResourcesUIModel extends IAncestorable {

	static final String PROP_BUILDS = "builds";
	static final String PROP_BUILD_CONFIGS = "buildConfigs";
	static final String PROP_DEPLOYMENT_CONFIGS = "deploymentConfigs";
	static final String PROP_IMAGE_STREAMS = "imageStreams";
	static final String PROP_IMAGE_STREAM_TAGS = "imageStreamTags";
	static final String PROP_PODS = "pods";
	static final String PROP_ROUTES = "routes";
	static final String PROP_REPLICATION_CONTROLLERS = "replicationControllers";
	static final String PROP_SERVICES = "services";
	static final String [] KINDS = new String []{
		ResourceKind.BUILD, 
		ResourceKind.BUILD_CONFIG,
		ResourceKind.DEPLOYMENT_CONFIG,
		ResourceKind.IMAGE_STREAM,
		ResourceKind.IMAGE_STREAM_TAG,
		ResourceKind.POD, 
		ResourceKind.ROUTE, 
		ResourceKind.REPLICATION_CONTROLLER,
		ResourceKind.SERVICE
	};
	
	Collection<IResourceUIModel> getBuilds();
	
	Collection<IResourceUIModel> getImageStreams();
	
	Collection<IResourceUIModel> getDeploymentConfigs();
	
	Collection<IResourceUIModel> getPods();
	
	Collection<IResourceUIModel> getRoutes();
	
	Collection<IResourceUIModel> getReplicationControllers();
	
	Collection<IResourceUIModel> getBuildConfigs();

	Collection<IResourceUIModel> getServices();
	
}
