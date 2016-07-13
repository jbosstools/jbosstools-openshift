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

import org.eclipse.core.runtime.IAdaptable;

/**
 * Shared implementation of all UI elements.
 * @author Thomas Mäder
 *
 * @param <R> the wrapped element type
 * @param <P> the parent type
 */
abstract class AbstractOpenshiftUIElement<R, P extends AbstractOpenshiftUIElement<?, ?>> implements IAdaptable, IOpenshiftUIElement<R, P> {
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
	
	public OpenshiftUIModel getRoot() {
		return getParent().getRoot();
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
		return wrapped.equals(other.getWrapped())
				&& (getParent() == null && other.getParent() == null || getParent().equals(other.getParent()));
	}
	
	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}
	
	synchronized void updateWith(R r) {
		wrapped= r;
		fireChanged();
	}
	

	protected void fireChanged() {
		fireChanged(this);
	}
	
	protected void fireChanged(IOpenshiftUIElement<?, ?> source) {
		getParent().fireChanged(source);
	}

	@Override
	public void refresh() {
		// since the containment structure depends on the content of a single
		// resource, the project is the project is the smallest unit where
		// refresh makes sense.
		getParent().refresh();
	}


}
