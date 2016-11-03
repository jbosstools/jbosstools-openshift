/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.jmx.jolokia.JolokiaConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;

public class OpenshiftJMXLabelProvider extends LabelProvider {

	public OpenshiftJMXLabelProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Image getImage(Object element) {
		return OpenShiftImages.OPENSHIFT_LOGO_IMG;
	}

	@Override
	public String getText(Object element) {
		return ((JolokiaConnectionWrapper)element).getName();
	}

}
