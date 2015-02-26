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
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.jboss.tools.openshift.common.core.connection.IConnection;

public class ConnectionPropertySource implements IPropertySource {
	
	private static final String HOST = "host";
	private static final String USERNAME = "username";
	private IConnection connection;

	public ConnectionPropertySource(IConnection connection) {
		this.connection = connection;
	}

	@Override
	public Object getEditableValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new TextPropertyDescriptor(HOST, "Host"),
				new TextPropertyDescriptor(USERNAME, "User Name")
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(HOST.equals(id)){
			return connection.toString();
		}
		if(USERNAME.equals(id))
			return connection.getUsername();
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id	, Object value) {
		
	}
}
