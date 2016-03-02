/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.ui.models.IProjectAdapter;
import org.jboss.tools.openshift.internal.ui.models.IProjectCache;
import org.jboss.tools.openshift.internal.ui.models.OpenShiftProjectCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

@RunWith(MockitoJUnitRunner.class)
public class OpenShiftProjectCacheTest {
	
	private OpenShiftProjectCache cache = new OpenShiftProjectCache();
	
	@Mock
	private IProject project;
	@Mock
	private IProject project2;

	@Mock 
	private TestConnection conn;
	@Mock
	private IProjectCache.IProjectCacheListener listener;
	
	@Before
	public void setUp() throws Exception {
		cache.addListener(listener);
		when(conn.getUsername()).thenReturn("aUser");
		when(conn.toString()).thenReturn("https://localhost:8443");
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(project));
	}
	
	@Test
	public void testGetProjectForOnlyMakesInitialCallToServer() {
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(new ArrayList<>());
		cache.getProjectsFor(conn);
		cache.getProjectsFor(conn);
		verify(conn, times(1)).getResources(ResourceKind.PROJECT);
	}
	
	@Test
	public void testConnectionRemoved() {
		cache.getProjectsFor(conn);
		cache.connectionRemoved(conn);
		thenListenersAreNotifiedOfRemoval(1);
	}
	
	@Test
	public void testConnectionChanged() {
		//That excludes loading projects from connection when adapters list is empty.
		//Loading would hide failure at firing project add, and cause failure at firing project remove.
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(Collections.emptyList());

		//project add
		cache.connectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS, Collections.emptyList(), Arrays.asList(project));

		List<IProjectAdapter> adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertAdapters(adapters);

		//project remove
		cache.connectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS, Arrays.asList(project), Collections.emptyList());
		adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertTrue(adapters.isEmpty());

		thenListenersAreNotifiedOfRemoval(1);
		thenListenersAreNotifiedOfAdd(1);
	}
	
	@Test
	public void testConnectionWithSeveralProjectsChanged() {
		when(project.getName()).thenReturn("project1");
		when(project2.getName()).thenReturn("project2");
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(project, project2));

		//provide initial loading 
		List<IProjectAdapter> adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertEquals(2, adapters.size());

		//project remove
		cache.connectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS, Arrays.asList(project, project2), Arrays.asList(project));
		adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertAdapters(adapters);

		//project add
		cache.connectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS, Arrays.asList(project), Arrays.asList(project, project2));

		adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertEquals(2, adapters.size());


		thenListenersAreNotifiedOfRemoval(1);
		thenListenersAreNotifiedOfAdd(3); //Twice at loading plus once at adding.
	}

	@Test
	public void testFlushForWhenUnknownConnection() {
		cache.flushFor(conn);
		thenListenersAreNotifiedOfAdd(0);
	}
	
	@Test
	public void testGetProjectsForAndFlush() {
		//lazy load
		List<IProjectAdapter> adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertAdapters(adapters);
		
		//cached load
		adapters = new ArrayList<>(cache.getProjectsFor(conn));
		assertAdapters(adapters);

		thenListenersAreNotifiedOfAdd(1);

		//flush connection
		cache.flushFor(conn);
		thenListenersAreNotifiedOfRemoval(1);
	}
	
	private void thenListenersAreNotifiedOfAdd(int times) {
		verify(listener, times(times)).handleAddToCache(any(IProjectCache.class), any(IProjectAdapter.class));
	}
	
	private void thenListenersAreNotifiedOfRemoval(int times) {
		verify(listener, times(times)).handleRemoveFromCache(any(IProjectCache.class), any(IProjectAdapter.class));
	}
	
	private void assertAdapters(List<IProjectAdapter> adapters) {
		assertEquals(1, adapters.size());
		assertEquals(project, adapters.get(0).getProject());
		assertEquals(conn, adapters.get(0).getParent());
	}
	
	public interface TestConnection extends IConnection, IOpenShiftConnection{ 
		
	}
}
