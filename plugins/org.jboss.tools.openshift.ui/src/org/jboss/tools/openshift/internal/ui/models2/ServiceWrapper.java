package org.jboss.tools.openshift.internal.ui.models2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public class ServiceWrapper extends ResourceContainer<IService, ProjectWrapper> {
	public ServiceWrapper(ProjectWrapper parent, IService resource) {
		super(parent, resource);
	}

	@Override
	public IService getResource() {
		return (IService) super.getResource();
	}
	
	void initWithResources(Collection<IResource> resources) {
		synchronized (childrenLock) {
			resources.forEach(r -> {
				containedResources.put(r, new ResourceWrapper(ServiceWrapper.this, r));
			});
		}
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
					AbstractResourceWrapper<?, ?> newWrapper = new ResourceWrapper(this, r);
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
	}

}
