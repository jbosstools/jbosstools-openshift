/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.openshift3.client.model.IResource;

public class DefaultResourcePropertySource extends ResourcePropertySource {

	protected DefaultResourcePropertySource(IResource resource) {
		super(resource);
	}

	@Override
	protected IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return null;
	}

}
