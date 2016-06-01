package org.jboss.tools.openshift.internal.ui.models2;

import org.eclipse.core.runtime.IAdaptable;

import com.openshift.restclient.model.IResource;

public abstract class AbstractResourceWrapper<R extends IResource, P extends IOpenshiftUIElement<?>> extends AbstractOpenshiftUIElement<P> implements IAdaptable, IOpenshiftUIElement<P> {
	private R resource;

	public AbstractResourceWrapper(P parent, R resource) {
		super(parent);
		this.resource = resource;
	}

	public synchronized IResource getResource() {
		return resource;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(resource)) {
			return (T) resource;
		}
		return null;
	}

	public synchronized void updateWith(IResource r) {
		if (OpenshiftUIModel.isOlder(getResource(), r)) {
			fireChanged(this);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO: implement
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		// TODO: implement
		return super.hashCode();
	}
}
