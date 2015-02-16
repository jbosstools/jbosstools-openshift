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

public class Assert {

	public static void assertPropertyDescriptors(IPropertyDescriptor[] exp, IPropertyDescriptor[] act){
		assertNotNull("Act is null", act);
		assertEquals("The array lengths are not the same",exp.length, act.length);
		List<String> actToString = new ArrayList<String>(exp.length);
		for (IPropertyDescriptor desc : act) {
			actToString.add(propertyDescriptorToString(desc));
		}
		for (IPropertyDescriptor desc : exp) {
			String expString = propertyDescriptorToString(desc);
			assertTrue(String.format("Exp. property descriptor: %s", expString), actToString.contains(expString));
		}
	}
	
	private static String propertyDescriptorToString(IPropertyDescriptor desc){
		ToStringBuilder builder = new ToStringBuilder(desc, StandardToStringStyle.SIMPLE_STYLE);
		return builder.toString();
	}
}
