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
package org.jboss.tools.openshift.express.test.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.ui.IActionFilter;
import org.jboss.tools.openshift.express.internal.ui.filters.ActionFilterAdapterFactory;
import org.jboss.tools.openshift.express.internal.ui.filters.SimplePropertyActionFilter;
import org.jboss.tools.openshift.express.test.core.ApplicationDetailsFake;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.IApplication;

/**
 * @author Jeff Cantrill
 */
public class ActionFilterAdapterFactoryTest {

	private ActionFilterAdapterFactory factory = new ActionFilterAdapterFactory();
	private IApplication app; 
	
	@Before
	public void Setup()	{
		app = new ApplicationDetailsFake(); 
	}
			
	@Test
	public void testGetAdapterList() {
		assertArrayEquals("Ext. the factory to support IActionFilter",new Class[] {IActionFilter.class},factory.getAdapterList());
	}
	
	@Test
	public void shouldOnlySupportIActionFilterAdapters() {
		assertNull(factory.getAdapter(app, String.class));
	}
	
	@Test
	public void testGetAdapterListForIApplication(){
		Object adapter = factory.getAdapter(app, IActionFilter.class);
		assertNotNull("Exp. to receive an adapter", adapter);
		assertTrue("Exp. an IActionFilter", adapter instanceof IActionFilter);
		assertTrue("Exp. a SimplePropertyActionFilter", adapter instanceof SimplePropertyActionFilter);
	}

}
