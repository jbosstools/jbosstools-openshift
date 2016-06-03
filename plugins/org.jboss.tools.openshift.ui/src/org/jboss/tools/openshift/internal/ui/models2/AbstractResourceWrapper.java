package org.jboss.tools.openshift.internal.ui.models2;

import org.eclipse.core.runtime.IAdaptable;

import com.openshift.restclient.model.IResource;

public abstract class AbstractResourceWrapper<R extends IResource, P extends IOpenshiftUIElement<?>> extends AbstractOpenshiftUIElement<P> implements IOpenshiftUIElement<P> {
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
		return super.getAdapter(adapter);
	}

	@SuppressWarnings("unchecked")
	public synchronized void updateWith(IResource r) {
		if (OpenshiftUIModel.isOlder(getResource(), r)) {
			resource= (R) r;
			fireChanged();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		if (o == this) {
			return true;
		}
		AbstractResourceWrapper<?, ?> other= (AbstractResourceWrapper<?, ?>) o;
		return resource.equals(other.getResource()) && getParent().equals(other.getParent());
	}
	
	@Override
	public int hashCode() {
		return resource.hashCode();
	}
}
