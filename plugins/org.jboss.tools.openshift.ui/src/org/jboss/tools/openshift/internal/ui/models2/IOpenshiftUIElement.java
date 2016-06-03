package org.jboss.tools.openshift.internal.ui.models2;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Common interface for OpenShift UI element wrappers
 * @author Thomas Mäder
 *
 * @param <P>
 */
public interface IOpenshiftUIElement<P extends IOpenshiftUIElement<?>> extends IAdaptable {
	public P getParent();
	default void fireChanged(IOpenshiftUIElement<?> source) {
		getParent().fireChanged(source);
	}
	
	default void fireChanged() {
		fireChanged(this);
	}
	
	default OpenshiftUIModel getRoot() {
		return getParent().getRoot();
	}
	
	@SuppressWarnings("unchecked")
	default <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(this)) {
			return (T) this;
		}
		return null;
	}

	
	default void refresh() {
		// TODO: implement refresh
	}
}
