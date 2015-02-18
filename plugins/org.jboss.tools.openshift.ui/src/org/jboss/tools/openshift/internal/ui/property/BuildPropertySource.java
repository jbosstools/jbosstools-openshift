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
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.openshift3.client.model.IBuild;

public class BuildPropertySource extends ResourcePropertySource<IBuild> {

	public BuildPropertySource(IBuild resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new TextPropertyDescriptor("podName", "Build Pod"),
				new TextPropertyDescriptor("message", "Build Message"),
				new TextPropertyDescriptor("status", "Status"),
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if("status".equals(id)){
			return getResource().getStatus();
		}
		if("message".equals(id)){
			return getResource().getMessage();
		}
		if("podName".equals(id)){
			return getResource().getPodName();
		}
		return super.getPropertyValue(id);
	}
	
}
