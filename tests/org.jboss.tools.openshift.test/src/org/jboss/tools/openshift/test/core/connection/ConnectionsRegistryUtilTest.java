/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.tools.openshift.core.connection.ConnectionNotFoundException;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.junit.Test;

import com.openshift.restclient.model.IResource;

public class ConnectionsRegistryUtilTest {
	
	@Test
	public void testConnectionNotFound() {
		String kind= "foo";
		String name = "bar";
		try {
			IResource resource = mock(IResource.class);
			when(resource.getKind()).thenReturn(kind);
			when(resource.getName()).thenReturn(name);
			ConnectionsRegistryUtil.getConnectionFor(resource );
			fail();
		} catch (ConnectionNotFoundException e) {
			assertEquals("Unable to find the connection for a "+kind+" named "+name, e.getMessage());
		}
		
	}
}

