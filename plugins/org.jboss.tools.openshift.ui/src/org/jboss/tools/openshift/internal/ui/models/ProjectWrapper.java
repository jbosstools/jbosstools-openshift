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
import java.util.concurrent.atomic.AtomicReference;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

class ProjectWrapper extends ResourceContainer<IProject, ConnectionWrapper> implements IProjectWrapper {
	private AtomicReference<LoadingState> state = new AtomicReference<LoadingState>(LoadingState.INIT);

	public ProjectWrapper(ConnectionWrapper parent, IProject resource) {
		super(parent, resource);
	}

	public LoadingState getState() {
		return state.get();
	}

	void setLoadingState(LoadingState newState) {
		state.set(newState);
	}

	public boolean load(IExceptionHandler handler) {
		if (state.compareAndSet(LoadingState.INIT, LoadingState.LOADING)) {
			getParent().startLoadJob(this, handler);
			return true;
		}
		return false;
	}

	protected void postUpdate(Collection<IResource> resources, Map<IResource, AbstractResourceWrapper<?, ?>> updated,
			boolean changed) {
		if (changed || !updated.isEmpty()) {
			// we need to update all services. Any resource change may have
			// changed to related
			getResources().forEach(wrapper -> {
				if (wrapper instanceof ServiceWrapper) {
					ServiceWrapper service = (ServiceWrapper) wrapper;
					Collection<IResource> relatedResources = ServiceResourceMapper
							.computeRelatedResources(service.getWrapped(), resources);
					service.updateWithResources(relatedResources);
				}
			});
		}
	}

	protected AbstractResourceWrapper<?, ?> createNewWrapper(Collection<IResource> resources, IResource r) {
		AbstractResourceWrapper<?, ?> newWrapper;
		if (r instanceof IService) {
			ServiceWrapper newService = new ServiceWrapper(ProjectWrapper.this, (IService) r);
			Collection<IResource> relatedResources = ServiceResourceMapper.computeRelatedResources((IService) r,
					resources);
			newService.initWithResources(relatedResources);
			newWrapper = newService;
		} else {
			newWrapper = new ResourceWrapper(ProjectWrapper.this, r);
		}
		return newWrapper;
	}

	void initWithResources(Collection<IResource> resources) {
		super.initWithResources(resources);
		state.set(LoadingState.LOADED);
	}

	@Override
	public void refresh() {
		getParent().refresh(this);
		state.set(LoadingState.LOADED);
		fireChanged();
	}
}
