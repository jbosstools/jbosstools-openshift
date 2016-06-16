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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.openshift.restclient.model.IResource;

abstract class ResourceContainer<R extends IResource, P extends AbstractOpenshiftUIElement<?, ?>>
		extends AbstractResourceWrapper<R, P> implements IResourceContainer<R, P> {
	private Map<IResource, AbstractResourceWrapper<?, ?>> containedResources = new HashMap<>();
	private Object childrenLock = new Object();

	public ResourceContainer(P parent, R resource) {
		super(parent, resource);
	}

	public Collection<IResourceWrapper<?, ?>> getResourcesOfKind(String kind) {
		return getResources().stream().filter(wrapper -> wrapper.getWrapped().getKind().equals(kind))
				.collect(Collectors.toSet());
	}
	
	public Collection<IResourceWrapper<?, ?>> getResources() {
		synchronized (childrenLock) {
			return new ArrayList<IResourceWrapper<?,?>>(containedResources.values());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResourceWrapper<?, ?>> Collection<T> getResourcesOfType(Class<T> clazz) {
		ArrayList<T> result= new ArrayList<>();
		for (IResourceWrapper<?, ?> r : getResources()) {
			if (clazz.isInstance(r)) {
				result.add((T) r);
			}
		}
		return result;
	}
	
	void updateWithResources(Collection<IResource> resources) {
		Map<IResource, AbstractResourceWrapper<?, ?>> updated = new HashMap<>();
		boolean changed = false;
		synchronized (childrenLock) {
			HashMap<IResource, AbstractResourceWrapper<?, ?>> oldWrappers = new HashMap<>(containedResources);
			containedResources.clear();
			for (IResource r : resources) {
				AbstractResourceWrapper<?, ?> existingWrapper = oldWrappers.remove(r);
				if (existingWrapper == null) {
					AbstractResourceWrapper<?, ?> newWrapper = createNewWrapper(resources, r);
					containedResources.put(r, newWrapper);
					changed = true;
				} else {
					containedResources.put(r, existingWrapper);
					updated.put(r, existingWrapper);
				}
			}
			if (!oldWrappers.isEmpty()) {
				changed = true;
			}
		}

		if (changed) {
			fireChanged();
		}
		updated.keySet().forEach(r -> {
			AbstractResourceWrapper<?, ?> wrapper = updated.get(r);
			wrapper.updateWith(r);
		});
		postUpdate(resources, updated, changed);
	}
	
	void initWithResources(Collection<IResource> resources) {
		synchronized (childrenLock) {
			resources.forEach(r -> {
				containedResources.put(r, createNewWrapper(resources, r));
			});
		}
	}

	protected abstract void postUpdate(Collection<IResource> resources, Map<IResource, AbstractResourceWrapper<?, ?>> updated, boolean changed);
	protected abstract AbstractResourceWrapper<?, ?> createNewWrapper(Collection<IResource> resources, IResource r);
}
