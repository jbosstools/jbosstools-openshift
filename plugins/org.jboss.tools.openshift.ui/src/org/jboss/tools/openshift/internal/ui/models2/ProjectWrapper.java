package org.jboss.tools.openshift.internal.ui.models2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

public class ProjectWrapper extends ResourceContainer<IProject, ConnectionWrapper> {
	private AtomicReference<LoadingState> state = new AtomicReference<LoadingState>(LoadingState.INIT);

	public ProjectWrapper(ConnectionWrapper parent, IProject resource) {
		super(parent, resource);
	}

	public Collection<AbstractResourceWrapper<?, ?>> getResources() {
		synchronized (childrenLock) {
			return containedResources.values();
		}
	}

	public LoadingState getState() {
		return state.get();
	}

	public void load() {
		if (state.compareAndSet(LoadingState.INIT, LoadingState.LOADING)) {
			getRoot().startLoadJob(this);
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
					AbstractResourceWrapper<?, ?> newWrapper;
					if (r instanceof IService) {
						ServiceWrapper newService = new ServiceWrapper(ProjectWrapper.this, (IService) r);
						Collection<IResource> relatedResources = ServiceResourceMapper
								.computeRelatedResources((IService) r, resources);
						newService.initWithResources(relatedResources);
						newWrapper = newService;
					} else {
						newWrapper = new ResourceWrapper(ProjectWrapper.this, r);
					}
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
			fireChanged(this);
		}

		updated.keySet().forEach(r -> {
			AbstractResourceWrapper<?, ?> wrapper = updated.get(r);
			wrapper.updateWith(r);
			if (wrapper instanceof ServiceWrapper) {
				ServiceWrapper service = (ServiceWrapper) wrapper;
				Collection<IResource> relatedResources = ServiceResourceMapper
						.computeRelatedResources(service.getResource(), resources);
				service.updateWithResources(relatedResources);
			}
		});
	}

	void initWithResources(Collection<IResource> resources) {
		synchronized (childrenLock) {
			for (IResource r : resources) {
				if (r instanceof IService) {
					ServiceWrapper wrapper = new ServiceWrapper(ProjectWrapper.this, (IService) r);
					Collection<IResource> relatedResources = ServiceResourceMapper
							.computeRelatedResources(wrapper.getResource(), resources);
					wrapper.initWithResources(relatedResources);
					containedResources.put(r, wrapper);
				} else {
					ResourceWrapper wrapper = new ResourceWrapper(this, r);
					containedResources.put(r, wrapper);
				}
			}
			state.set(LoadingState.LOADED);
		}
	}

}
