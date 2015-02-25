/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property;

import static org.junit.Assert.*;
import static org.jboss.tools.openshift.test.ui.property.Assert.*;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.junit.Before;
import org.junit.Test;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.property.ConnectionPropertySource;

public class ConnectionPropertySourceTest {
	
	private Connection connection;
	private ConnectionPropertySource source;
	
	@Before
	public void setup() throws Exception{
		connection = new Connection("http://localhost:8080", null);
		source = new ConnectionPropertySource(connection);
	}
	
	@Test
	public void getPropertyValue(){
		assertEquals("http://localhost:8080", source.getPropertyValue("host"));
		assertEquals("foo", source.getPropertyValue("username"));
	}
	
	@Test
	public void getPropertyDescriptor() {
		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new TextPropertyDescriptor("host", "Host"), 
				new TextPropertyDescriptor("username", "User Name")
		};
		assertPropertyDescriptorsEquals(exp, source.getPropertyDescriptors());
	}
}
