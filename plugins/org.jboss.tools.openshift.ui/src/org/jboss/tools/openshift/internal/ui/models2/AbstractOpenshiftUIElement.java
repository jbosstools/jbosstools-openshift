package org.jboss.tools.openshift.internal.ui.models2;

import org.eclipse.core.runtime.IAdaptable;

public abstract class AbstractOpenshiftUIElement<P extends IOpenshiftUIElement<?>> implements IAdaptable, IOpenshiftUIElement<P> {
	private P parent;

	public AbstractOpenshiftUIElement(P parent) {
		this.parent = parent;
	}

	public P getParent() {
		return parent;
	}
}
