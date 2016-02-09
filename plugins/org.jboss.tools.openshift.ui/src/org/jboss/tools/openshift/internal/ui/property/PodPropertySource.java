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

import com.openshift.restclient.model.IPod;

public class PodPropertySource extends ResourcePropertySource<IPod> {

	public PodPropertySource(IPod resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new UneditablePropertyDescriptor("ip", "IP"),
				new UneditablePropertyDescriptor("host", "Host"),
				new UneditablePropertyDescriptor("images", "Image(s)"),
				new UneditablePropertyDescriptor("status", "Status"),
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if("status".equals(id)){
			return getResource().getStatus();
		}
		if("ip".equals(id)){
			return getResource().getIP();
		}
		if("host".equals(id)){
			return getResource().getHost();
		}
		if("images".equals(id)){
			return  org.apache.commons.lang.StringUtils.join(getResource().getImages(), ", ");
		}
		return super.getPropertyValue(id);
	}
	
}
