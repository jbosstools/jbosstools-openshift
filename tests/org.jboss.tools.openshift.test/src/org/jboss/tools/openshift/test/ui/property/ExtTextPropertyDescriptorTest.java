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

import org.jboss.tools.openshift.internal.ui.property.ExtTextPropertyDescriptor;
import org.junit.Test;

public class ExtTextPropertyDescriptorTest {

	@Test
	public void testCategoryIsSet() {
		assertEquals("Common", new ExtTextPropertyDescriptor("foo", "bar", "Common").getCategory());
	}
	
	@Test
	public void testDisplayNameFromEnum(){
		ExtTextPropertyDescriptor descriptor = new ExtTextPropertyDescriptor(Foo.Bar, "foo");
		assertEquals("Bar", descriptor.getDisplayName());
		assertEquals("foo", descriptor.getCategory());
	}
	
	private enum Foo{
		Bar;
	}
	
}
