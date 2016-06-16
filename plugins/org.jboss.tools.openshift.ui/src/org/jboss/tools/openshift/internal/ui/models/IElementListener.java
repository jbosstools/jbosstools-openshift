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

/**
 * Listener interface for changes to elements of the openshift 3 ui model.
 * Notifications will happen on the UI thread.
 * 
 * @author Thomas Mäder
 */
public interface IElementListener {
	/**
	 * Notification that the given element has changed in some way. It is up to
	 * the listener to determine what exactly has changed.
	 * 
	 * @param element
	 *            the changed element.
	 */
	void elementChanged(IOpenshiftUIElement<?, ?> element);
}
