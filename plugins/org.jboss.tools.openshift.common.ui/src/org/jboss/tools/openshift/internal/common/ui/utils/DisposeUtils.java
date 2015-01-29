/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.core.databinding.Binding;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;

public class DisposeUtils {

	public static boolean isDisposed(Control control) {
		return control == null
				|| control.isDisposed();
	}
	
	public static boolean isDisposed(Viewer viewer) {
		return viewer == null
				|| isDisposed(viewer.getControl());
	}

	public static boolean isDisposed(Binding binding) {
		return binding == null
				|| binding.isDisposed();
	}
}
