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

import com.openshift.restclient.model.IImageStream;

/**
 * @author jeff.cantrill
 */
public class ImageStreamPropertySource extends ResourcePropertySource<IImageStream> {

	private static final String REGISTRY = "registry";

	public ImageStreamPropertySource(IImageStream resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new UneditablePropertyDescriptor(REGISTRY, "Registry")
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(REGISTRY.equals(id)){
			return getResource().getDockerImageRepository();
		}
		return super.getPropertyValue(id);
	}
	
}
