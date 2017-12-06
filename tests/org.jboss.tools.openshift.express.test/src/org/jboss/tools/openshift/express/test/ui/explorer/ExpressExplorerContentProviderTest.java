/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.ui.explorer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.explorer.ExpressExplorerContentProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;

@RunWith(MockitoJUnitRunner.class)
public class ExpressExplorerContentProviderTest {

	private ExpressExplorerContentProvider provider;
	private ConnectionsRegistry registry;
	@Mock
	private IApplication application;
	@Mock
	private IDomain domain;
	@Mock
	private IConnection connection;

	@Before
	public void setup() {
		provider = new ExpressExplorerContentProvider();
		registry = new ConnectionsRegistry();
		registry.add(connection);
	}

	@Test
	public void getExplorerElementsReturnsDomainsForExpressConnections() {
		ExpressConnection xpressConnection = spy(new ExpressConnection("", "http://localhost"));
		registry.add(xpressConnection);
		List<IDomain> domains = Arrays.asList(domain);
		doReturn(domains).when(xpressConnection).getDomains();

		assertArrayEquals(domains.toArray(), provider.getExplorerElements(xpressConnection));
	}

	@Test
	public void getElementsReturnsExpressConnectionsForTheRegistry() {
		registry.add(new ExpressConnection("", "http://localhost"));
		registry.add(new ExpressConnection("", "http://localhost:8080"));

		Object[] elements = provider.getExplorerElements(registry);
		assertEquals("Exp. only ExpressConnections to be returned by this provider", 2, elements.length);
		for (Object e : elements) {
			if (!(e instanceof ExpressConnection)) {
				fail("Exp. only ExpressConnections to be returned by this provider");
			}
		}
	}

	@Test
	public void testRegistryHasChildren() {
		assertTrue("Exp. the ConnectionsRegistry to have children", provider.hasChildren(registry));
	}

	@Test
	public void testConnectionHasChildren() {
		assertTrue("Exp. an IConnection to have children", provider.hasChildren(connection));
	}

	@Test
	public void testDomainHasChildren() {
		assertTrue("Exp. an IDomain to have children", provider.hasChildren(domain));
	}

	@Test
	public void testApplicationHasChildren() {
		assertTrue("Exp. an IApplication to have children", provider.hasChildren(application));
	}
}
