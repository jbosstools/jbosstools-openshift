package org.jboss.tools.openshift.internal.ui.models2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.openshift.restclient.model.IResource;

public abstract class ResourceContainer<R extends IResource, P extends IOpenshiftUIElement<?>>
		extends AbstractResourceWrapper<R, P> {
	protected Map<IResource, AbstractResourceWrapper<?, ?>> containedResources = new HashMap<>();
	protected Object childrenLock = new Object();

	public ResourceContainer(P parent, R resource) {
		super(parent, resource);
	}

	public Collection<AbstractResourceWrapper<?, ?>> getResourcesOfKind(String kind) {
		return getResources().stream().filter(wrapper -> wrapper.getResource().getKind().equals(kind))
				.collect(Collectors.toSet());
	}

	public Collection<AbstractResourceWrapper<?, ?>> getResources() {
		synchronized (childrenLock) {
			return containedResources.values();
		}
	}
}
