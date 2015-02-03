/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.ExplorerContentProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift3.client.model.IProject;

@RunWith(MockitoJUnitRunner.class)
public class ExplorerContentProviderTest {

	private ConnectionsRegistry registry;
	private ExplorerContentProvider provider;
	private Connection connection;
	@Mock
	private IProject project;
	
	@Before
	public void setup(){
		registry = new ConnectionsRegistry();
		connection = spy(new Connection("http://localhost:8080"));
		registry.add(connection);
		provider = new ExplorerContentProvider();
	}
	
	@Test
	public void getChildrenForConnectionReturnsProjects(){
		List<IProject> projects = Arrays.asList(new IProject[]{project});
//		doReturn(projects).when(connection).getProjects();
		assertArrayEquals("Exp. to get all the projects for a Connection", projects.toArray(),  provider.getChildren(connection));
	}

	@Test
	public void getElementsForRegistryReturnsConnections(){
		assertArrayEquals("Exp. to get all the connections from the ConnectionsRegistry", new Object []{connection},  provider.getElements(registry));
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
		assertTrue("Exp. #hasChildren to return true for IProject", provider.hasChildren(project));
	}

}
