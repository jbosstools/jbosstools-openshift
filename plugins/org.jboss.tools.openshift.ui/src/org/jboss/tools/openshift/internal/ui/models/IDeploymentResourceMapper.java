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

import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.openshift.common.core.IRefreshable;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public interface IDeploymentResourceMapper extends IObservablePojo, IRefreshable {

	Collection<Deployment> getDeployments();
	
	/**
	 * Retrieve the ImageStreamTags associated with the given
	 * service.  This will most likely trigger a call to the
	 * server since ImageStreamTags are not a watchable resource
	 * 
	 * @param service
	 * @return The collection of imagestreamtags or an empty collection
	 */
	Collection<IResource> getImageStreamTagsFor(IService service);
}
