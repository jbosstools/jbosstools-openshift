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
import java.util.Map;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

class ServiceWrapper extends ResourceContainer<IService, ProjectWrapper> implements IServiceWrapper {
	public ServiceWrapper(ProjectWrapper parent, IService resource) {
		super(parent, resource);
	}

	protected void postUpdate(Collection<IResource> resources, Map<IResource, AbstractResourceWrapper<?, ?>> updated,
			boolean changed) {
		// do nothing
		
	}

	protected ResourceWrapper createNewWrapper(Collection<IResource> resources, IResource r) {
		return new ResourceWrapper(this, r);
	}
}
