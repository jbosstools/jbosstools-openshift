/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerContentProvider;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenShiftExplorerContentProviderTest {

	private OpenShiftExplorerContentProvider provider;
	private Connection connection;
	private ConnectionsRegistry registry;
	@Mock private IProject project;
	@Mock private IClient client;
	
	@Before
	public void setup() throws Exception{
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8442")); 

		connection = spy(new Connection(client, null, null));
		doReturn(true).when(connection).ownsResource(any(IResource.class));
		doReturn("hookaboo").when(connection).getUsername();
		
		registry = ConnectionsRegistrySingleton.getInstance();
		registry.add(connection);
		
		provider = new OpenShiftExplorerContentProvider();
	}
	
	@After
	public void teardown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}
	
	@Test
	public void getChildrenForConnectionReturnsProjectAdapters(){
		List<IProject> projects = Arrays.asList(new IProject[]{project});
		doReturn(projects).when(connection).getResources(anyString());
		
		assertArrayEquals("Exp. to get all the projects for a Connection", projects.toArray(),  Arrays.asList(provider.getChildrenFor(connection)).stream().map(a->((IProjectAdapter)a).getProject()).toArray());
	}

	@Test
	public void getExplorerElementsForRegistryReturnsConnections(){
		assertArrayEquals("Exp. to get all the connections from the ConnectionsRegistry", new Object []{connection},  provider.getExplorerElements(registry));
	}

	@Test
	public void connectionsRegistryShouldHaveChildren(){
		assertTrue("Exp. #hasChildren to return true for ConnectionsRegistry", provider.hasChildren(registry));
	}
	@Test
	public void connectionsShouldHaveChildren(){
		assertTrue("Exp. #hasChildren to return true for Connections", provider.hasChildren(connection));
	}
	@Test
	public void projectsShouldHaveChildren(){
		assertTrue("Exp. #hasChildren to return true for IProject", provider.hasChildren(mock(IProjectAdapter.class)));
	}

}
