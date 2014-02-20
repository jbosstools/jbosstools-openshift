/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.viewers.IElementComparer;

/**
 * Viewer element comparer based on #equals(). The default implementation in
 * CheckboxTableViewer compares elements based on instance identity.
 * <p>
 * We need this since the available cartridges (item listed in the viewer)
 * are not the same instance as the ones in the embedded application (items
 * to check in the viewer).
 * 
 * @author Andre Dietisheim
 */
public class EqualityComparer implements IElementComparer {

	@Override
	public boolean equals(Object thisObject, Object thatObject) {
		if (thisObject == null) {
			return thatObject != null;
		}

		if (thatObject == null) {
			return false;
		}

		return thisObject.equals(thatObject);
	}

	@Override
	public int hashCode(Object element) {
		return element.hashCode();
	}

}
