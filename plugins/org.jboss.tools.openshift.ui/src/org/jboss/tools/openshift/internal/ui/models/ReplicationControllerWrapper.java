/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
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

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;

class ReplicationControllerWrapper extends ResourceContainer<IReplicationController, ProjectWrapper> implements IReplicationControllerWrapper {
	public ReplicationControllerWrapper(ProjectWrapper parent, IReplicationController resource) {
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
