/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * @author Jeff Cantrill
 */
public class Assert {

	public static void assertPropertyDescriptorsContains(IPropertyDescriptor[] expected, IPropertyDescriptor[] actual){
		List<String> actualToString = new ArrayList<>(actual.length);
		for (IPropertyDescriptor descriptor : actual) {
			actualToString.add(propertyDescriptorToString(descriptor));
		}
		for (IPropertyDescriptor desc : expected) {
			String expectedString = propertyDescriptorToString(desc);
			assertTrue(String.format("Expected property descriptor: %s", expectedString), actualToString.contains(expectedString));
		}
	}

	public static void assertPropertyDescriptorsEquals(IPropertyDescriptor[] expected, IPropertyDescriptor[] actual){
		assertNotNull("Actual value is null", actual);
		assertEquals("The array lengths are not the same",expected.length, actual.length);
		assertPropertyDescriptorsContains(expected, actual);
	}
	
	private static String propertyDescriptorToString(IPropertyDescriptor descriptor){
		return new ToStringBuilder(descriptor, StandardToStringStyle.SIMPLE_STYLE)
		.append("category",descriptor.getCategory())
		.append("id",descriptor.getId())
		.append("displayname", descriptor.getDisplayName()).toString();
	}
}
