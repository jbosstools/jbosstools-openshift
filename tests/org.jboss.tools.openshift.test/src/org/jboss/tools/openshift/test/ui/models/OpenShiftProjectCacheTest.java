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

import static org.jboss.tools.openshift.test.core.connection.ConnectionTestUtils.createConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.ui.models.IConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.LoadingState;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.test.util.UITestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

@RunWith(MockitoJUnitRunner.class)
public class OpenShiftProjectCacheTest {

	private IConnectionWrapper connectionWrapper;
	private OpenshiftUIModel root;

	@Mock
	private IProject project;
	@Mock
	private IProject project2;

	private Connection conn;

	@Before
	public void setUp() throws Exception {
		this.conn = spy(createConnection("aUser", "123", "https://localhost:8443"));
		doReturn(Arrays.asList(project)).when(conn).getResources(ResourceKind.PROJECT);
		ConnectionsRegistrySingleton.getInstance().clear();
		ConnectionsRegistrySingleton.getInstance().add(conn);
		root = new OpenshiftUIModel();
		connectionWrapper = root.getConnections().iterator().next();
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void testGetProjectForOnlyMakesInitialCallToServer() throws InterruptedException, TimeoutException {
		IOpenShiftConnection connection = connectionWrapper.getWrapped();
		connectionWrapper.load(IExceptionHandler.NULL_HANDLER);
		connectionWrapper.load(IExceptionHandler.NULL_HANDLER);
		UITestUtils.waitForState(connectionWrapper, LoadingState.LOADED);
		verify(connection, times(1)).getResources(ResourceKind.PROJECT);
	}

	@Test
	public void testConnectionChanged() {
		// That excludes loading projects from connection when adapters list is
		// empty.
		// Loading would hide failure at firing project add, and cause failure
		// at firing project remove.
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(Collections.emptyList());

		// project add
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS,
				Collections.emptyList(), Arrays.asList(project));

		Collection<IResourceWrapper<?, ?>> adapters = connectionWrapper.getResources();
		assertAdapters(adapters);

		// project remove
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS,
				Arrays.asList(project), Collections.emptyList());
		adapters = connectionWrapper.getResources();
		assertTrue(adapters.isEmpty());
	}

	@Test
	public void testConnectionWithSeveralProjectsChanged() throws InterruptedException, TimeoutException {
		when(project.getName()).thenReturn("project1");
		when(project2.getName()).thenReturn("project2");
		when(conn.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(project, project2));

		// provide initial loading
		connectionWrapper.load(IExceptionHandler.NULL_HANDLER);
		UITestUtils.waitForState(connectionWrapper, LoadingState.LOADED);
		Collection<IResourceWrapper<?, ?>> adapters = connectionWrapper.getResources();
		assertEquals(2, adapters.size());

		// project remove
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS,
				Arrays.asList(project, project2), Arrays.asList(project));
		adapters = connectionWrapper.getResources();
		assertAdapters(adapters);

		// project add
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(conn, ConnectionProperties.PROPERTY_PROJECTS,
				Arrays.asList(project), Arrays.asList(project, project2));

		adapters = connectionWrapper.getResources();
		assertEquals(2, adapters.size());
	}

	private void assertAdapters(Collection<IResourceWrapper<?, ?>> adapters) {
		assertEquals(1, adapters.size());
		IResourceWrapper<?, ?> r= adapters.iterator().next();
		assertEquals(project, r.getWrapped());
		assertEquals(conn, r.getParent().getWrapped());
	}
}
