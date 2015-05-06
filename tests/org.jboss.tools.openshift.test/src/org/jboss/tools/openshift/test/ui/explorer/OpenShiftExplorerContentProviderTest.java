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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerContentProvider;
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenShiftExplorerContentProviderTest {

	private ConnectionsRegistry registry;
	private OpenShiftExplorerContentProvider provider;
	private Connection connection;
	@Mock private IProject project;
	@Mock private IClient client;
	
	@Before
	public void setup() throws Exception{
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8442")); 
		registry = new ConnectionsRegistry();
		connection = new Connection(client, null, null, null);
		registry.add(connection);
		provider = new OpenShiftExplorerContentProvider();
	}
	
	private ResourceGrouping givenAResourceGroup(){
		ArrayList<IResource> resources = new ArrayList<IResource>();
		resources.add(mock(IService.class));
		when(project.getResources(ResourceKind.Service)).thenReturn(resources);
		ResourceGrouping group = new ResourceGrouping(ResourceKind.Service, project);
		return group;
	}
	@Test
	public void getChildrenForResourceGroupReturnsResources(){
		ResourceGrouping group = givenAResourceGroup();
		
		assertArrayEquals("Exp. to get the resources associated with a group", group.getResources(), provider.getChildrenFor(group));
	}
	
	@Test
	public void getChildrenForProjectReturnsResourceGroups(){
		when(project.getResources(any(ResourceKind.class))).thenReturn(new ArrayList<IResource>());

		ResourceGrouping [] groups = new ResourceGrouping[]{
				new ResourceGrouping(ResourceKind.BuildConfig, project),
				new ResourceGrouping(ResourceKind.DeploymentConfig, project),
				new ResourceGrouping(ResourceKind.Service, project),
				new ResourceGrouping(ResourceKind.Pod, project),
				new ResourceGrouping(ResourceKind.ReplicationController, project),
				new ResourceGrouping(ResourceKind.Build, project),
				new ResourceGrouping(ResourceKind.ImageStream, project),
				new ResourceGrouping(ResourceKind.Route, project),
		};
		
		Object[] children = provider.getChildren(project);
		assertArrayEquals("Exp. to get a set of resource groups for a project", groups, children);
	}
	
	@Test
	public void getChildrenForConnectionReturnsProjects(){
		List<IProject> projects = Arrays.asList(new IProject[]{project});
		when(client.<IProject>list(ResourceKind.Project)).thenReturn(projects);
		
		assertArrayEquals("Exp. to get all the projects for a Connection", projects.toArray(),  provider.getChildrenFor(connection));
	}

	@Test
	public void getExplorerElementsForRegistryReturnsConnections(){
		assertArrayEquals("Exp. to get all the connections from the ConnectionsRegistry", new Object []{connection},  provider.getExplorerElements(registry));
	}

	@Test
	public void resourceGroupingsShouldHaveChildrenWhenTheyHaveNonEmptyList(){
		ResourceGrouping group = givenAResourceGroup();
		assertTrue("Exp. #hasChildren to return true for ResourceGrouping with resources", provider.hasChildren(group));
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
