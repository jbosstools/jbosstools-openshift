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

import com.openshift3.client.IClient;
import com.openshift3.client.ResourceKind;
import com.openshift3.client.model.IProject;
import com.openshift3.client.model.IResource;
import com.openshift3.client.model.IService;

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
		connection = new Connection(client);
		registry.add(connection);
		provider = new OpenShiftExplorerContentProvider();
	}
	
	private ResourceGrouping givenAResourceGroup(){
		ArrayList<IResource> resources = new ArrayList<IResource>();
		resources.add(mock(IService.class));
		ResourceGrouping group = new ResourceGrouping(ResourceKind.Service, resources);
		return group;
	}
	@Test
	public void getChildrenForResourceGroupReturnsResources(){
		ResourceGrouping group = givenAResourceGroup();
		
		assertArrayEquals("Exp. to get the resources associated with a group", group.getResources(), provider.getChildren(group));
	}
	
	@Test
	public void getChildrenForProjectReturnsResourceGroups(){
		when(project.getResources(any(ResourceKind.class))).thenReturn(new ArrayList<IResource>());

		ResourceGrouping [] groups = new ResourceGrouping[]{
				new ResourceGrouping(ResourceKind.BuildConfig, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.DeploymentConfig, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.Service, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.Pod, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.ReplicationController, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.Build, new ArrayList<IResource>()),
				new ResourceGrouping(ResourceKind.ImageRepository, new ArrayList<IResource>()),
		};
		
		Object[] children = provider.getChildren(project);
		assertArrayEquals("Exp. to get a set of resource groups for a project", groups, children);
	}
	
	@Test
	public void getChildrenForConnectionReturnsProjects(){
		List<IProject> projects = Arrays.asList(new IProject[]{project});
		when(client.<IProject>list(ResourceKind.Project)).thenReturn(projects);
		
		assertArrayEquals("Exp. to get all the projects for a Connection", projects.toArray(),  provider.getChildren(connection));
	}

	@Test
	public void getElementsForRegistryReturnsConnections(){
		assertArrayEquals("Exp. to get all the connections from the ConnectionsRegistry", new Object []{connection},  provider.getElements(registry));
	}

	@Test
	public void resourceGroupingsShouldHaveChildrenWhenTheyHaveNonEmptyList(){
		ResourceGrouping group = givenAResourceGroup();
		assertTrue("Exp. #hasChildren to return true for ResourceGrouping with resources", provider.hasChildren(group));
	}

	@Test
	public void resourceGroupingsShouldNotHaveChildrenWhenItHasAnEmptyList(){
		ResourceGrouping group = new ResourceGrouping(ResourceKind.Service, new ArrayList<IResource>());
		assertFalse("Exp. #hasChildren to return false for ResourceGrouping with no resources", provider.hasChildren(group));
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
