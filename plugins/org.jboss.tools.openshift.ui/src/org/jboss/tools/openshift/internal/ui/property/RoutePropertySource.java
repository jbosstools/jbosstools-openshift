/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.openshift.restclient.model.route.IRoute;

/**
 * @author jeff.cantrill
 */
public class RoutePropertySource extends ResourcePropertySource<IRoute> {

	private static final String SERVICE = "service";
	private static final String HOST_PATH = "host/path";

	public RoutePropertySource(IRoute resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new TextPropertyDescriptor(HOST_PATH, "URI"),
				new TextPropertyDescriptor(SERVICE, "Service"),
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(HOST_PATH.equals(id)){
			return NLS.bind("{0}{1}", getResource().getHost(), getResource().getPath());
		}
		if(SERVICE.equals(id)){
			return getResource().getServiceName();
		}
		return super.getPropertyValue(id);
	}
	
}
