/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.beans.PropertyChangeListener;

import org.jboss.tools.common.databinding.IObservablePojo;

/**
 * @author Andre Dietisheim
 */
public class ObservablePojoUtils {

	private ObservablePojoUtils() {
	}

	public static void addPropertyChangeListener(PropertyChangeListener listener, Object object) {
		if (!(object instanceof IObservablePojo)) {
			return;
		}
		((IObservablePojo) object).addPropertyChangeListener(listener);
	}

	public static void removePropertyChangeListener(PropertyChangeListener listener, Object object) {
		if (listener != null
				&& object instanceof IObservablePojo) {
			((IObservablePojo) object).removePropertyChangeListener(listener);
		}
	}

	
}
