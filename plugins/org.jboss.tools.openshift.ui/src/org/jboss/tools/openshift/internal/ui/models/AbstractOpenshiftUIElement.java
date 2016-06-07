package org.jboss.tools.openshift.internal.ui.models;

import org.eclipse.core.runtime.IAdaptable;

public abstract class AbstractOpenshiftUIElement<R, P extends IOpenshiftUIElement<?>> implements IAdaptable, IOpenshiftUIElement<P> {
	private P parent;
	private R wrapped;

	public AbstractOpenshiftUIElement(P parent, R wrapped) {
		this.parent = parent;
		this.wrapped= wrapped;
	}

	public P getParent() {
		return parent;
	}
	
	public R getWrapped() {
		return wrapped;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(this)) {
			return (T) this;
		} else if (adapter.isInstance(wrapped)) {
			return (T) wrapped;
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		if (o == this) {
			return true;
		}
		AbstractOpenshiftUIElement<?, ?> other= (AbstractOpenshiftUIElement<?, ?>) o;
		return wrapped.equals(other.getWrapped()) && getParent().equals(other.getParent());
	}
	
	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}
	
	public synchronized void updateWith(R r) {
		wrapped= r;
		fireChanged();
	}
	
	@Override
	public void refresh() {
		// since the containment structure depends on the content of a single
		// resource, the project is the project is the smallest unit where
		// refresh makes sense.
		getParent().refresh();
	}


}
