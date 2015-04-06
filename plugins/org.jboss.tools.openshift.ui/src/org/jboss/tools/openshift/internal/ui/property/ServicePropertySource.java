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
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IService;

public class ServicePropertySource extends ResourcePropertySource<IService>{

	public ServicePropertySource(IService resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new TextPropertyDescriptor("selector", "Selector"),
				new TextPropertyDescriptor("port", "Port"),
				new TextPropertyDescriptor("portalIp", "IP"),
				new TextPropertyDescriptor("containerPort", "Container Port")
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if("portalIp".equals(id)) return getResource().getPortalIP();
		if("containerPort".equals(id)) return getResource().getContainerPort();
		if("selector".equals(id)){
			return StringUtils.serialize(getResource().getSelector());
		}
		if("port".equals(id))
			return getResource().getPort();
		return super.getPropertyValue(id);
	}
	
}
