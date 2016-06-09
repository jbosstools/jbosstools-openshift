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
 * Common interface for OpenShift UI element wrappers.
 * 
 * @author Thomas Mäder
 *
 * @param <P> the parent type
 * @param <R> the type of the wrapped element
 */
public interface IOpenshiftUIElement<R, P extends IOpenshiftUIElement<?, ?>> extends IAdaptable {
	/**
	 * Get the container of this element. 
	 * @return
	 */
	public P getParent();
	public R getWrapped();

	@SuppressWarnings("unchecked")
	default <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(this)) {
			return (T) this;
		}
		return null;
	}

	/**
	 * Update the state of the given element with the latest version from
	 * openshift.
	 */
	void refresh();

	public OpenshiftUIModel getRoot();
}
