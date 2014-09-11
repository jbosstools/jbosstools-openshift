/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.ui.property;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.express.internal.ui.property.ApplicationPropertySource;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.IApplication;

import static org.jboss.tools.openshift.express.test.mocks.ApplicationMocks.*;

public class ApplicationPropertySourceTest {
	
	private IApplication app;
	private ApplicationPropertySource propertySource;
	
	@Before
	public void setup(){
		app = givenAnApplication(); 
		propertySource = new ApplicationPropertySource(app);
	}

	@Test
	public void testGetPropertyDescriptors() {
		String [] exp = new String [] {
				"Created on", "Public URL", "Name", "UUID", "Git URL", "Type", "Port Forwarding", "Scalable"
		};
		assertPropertyDescriptors(exp,propertySource.getPropertyDescriptors());
	}
	
	@Test
	public void testGetPropertyValueForScalable(){
		assertEquals("", app.getApplicationScale().getValue(), propertySource.getPropertyValue("8.Scalable"));
	}
	
	private void assertPropertyDescriptors(String [] properties, IPropertyDescriptor [] descriptors){
		String [] actuals = new String [descriptors.length] ;
		for (int i = 0; i < descriptors.length; i++) {
			actuals[i] = descriptors[i].getDisplayName();
		}
		Arrays.sort(properties);
		Arrays.sort(actuals);
		assertArrayEquals("Exp. certain set of property descriptors", properties, actuals);
	}
}
