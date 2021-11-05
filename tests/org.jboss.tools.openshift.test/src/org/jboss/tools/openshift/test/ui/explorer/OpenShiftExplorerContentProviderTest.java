/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerContentProvider;
import org.jboss.tools.openshift.internal.ui.models.IConnectionWrapper;
import org.jboss.tools.openshift.internal.ui.models.IElementListener;
import org.jboss.tools.openshift.internal.ui.models.IExceptionHandler;
import org.jboss.tools.openshift.internal.ui.models.IOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.LoadingState;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;
import org.jboss.tools.openshift.test.core.connection.ConnectionTestUtils;
import org.jboss.tools.openshift.test.util.UITestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OpenShiftExplorerContentProviderTest {

	private static final String OPENSHIFT_SERVER_URL = "https://localhost:8442";

	private OpenShiftExplorerContentProvider provider;
	private Connection connection;
	private IConnectionWrapper connectionWrapper;
	private OpenshiftUIModel model;
	private ConnectionsRegistry registry;
	@Mock
	private IProject project;

	@Before
	public void setup() throws Exception {
		this.connection = spy(ConnectionTestUtils.createConnection("auser", "atoken", OPENSHIFT_SERVER_URL));
		doReturn(true).when(connection).ownsResource(any(IResource.class));
		this.registry = ConnectionsRegistrySingleton.getInstance();
		registry.clear();
		registry.add(connection);

		this.model = new OpenshiftUIModel(registry) {
		};
		this.connectionWrapper = model.getConnections().iterator().next();
		this.provider = new OpenShiftExplorerContentProvider(model) {
		};
	}

	@Test
	public void getChildrenForConnectionReturnsProjectAdapters() throws InterruptedException, TimeoutException {
		List<IProject> projects = Arrays.asList(new IProject[] { project });
		doReturn(projects).when(connection).getResources(anyString());
		connectionWrapper.load(IExceptionHandler.NULL_HANDLER);
		UITestUtils.waitForState(connectionWrapper, LoadingState.LOADED);

		assertArrayEquals("Exp. to get all the projects for a Connection", projects.toArray(),
				Arrays.asList(provider.getChildren(connectionWrapper)).stream()
						.map(a -> ((IProjectWrapper) a).getWrapped()).toArray());
	}

	@Test
	public void getExplorerElementsForRegistryReturnsConnections() {
		assertArrayEquals("Exp. to get all the connections from the ConnectionsRegistry",
				new Object[] { connectionWrapper }, provider.getElements(registry));
	}

	@Test
	public void connectionsRegistryShouldHaveChildren() {
		assertTrue("Exp. #hasChildren to return true for ConnectionsRegistry", provider.hasChildren(model));
	}

	@Test
	public void connectionsShouldHaveChildren() {
		assertTrue("Exp. #hasChildren to return true for Connections", provider.hasChildren(connectionWrapper));
	}

	@Test
	public void projectsShouldHaveChildren() {
		assertTrue("Exp. #hasChildren to return true for IProject", provider.hasChildren(mock(IProjectWrapper.class)));
	}

	@Test
	public void modelShouldHaveConnectionThatIsAddedInRegistry() throws MalformedURLException {
		// given
		int numOfConnections = model.getConnections().size();
		Connection connection2 = ConnectionTestUtils.createConnection("aUser", "123456", "https://127.0.0.1:8080");
		// when
		registry.add(connection2);
		// then
		assertThat(model.getConnections()).hasSize(numOfConnections + 1);
	}

	@Test
	public void modelShouldRemoveConnectionIfItIsRemovedFromRegistry() throws InterruptedException {
		// given
		int numOfConnections = model.getConnections().size();
		// when
		registry.remove(connection);
		// then
		assertThat(model.getConnections()).hasSize(numOfConnections - 1);
	}

	@Test
	public void modelShouldNotifyConnectionAddedInRegistry() throws InterruptedException, MalformedURLException {
		// given
		Connection connection2 = ConnectionTestUtils.createConnection("anotherUser", "654321",
				"https://127.0.0.1:8181");
		IElementListener listener = spy(new VoidElementListener());
		model.addListener(listener);
		// when
		registry.add(connection2);
		// then
		verify(listener, timeout(10 * 1000)).elementChanged(any());
	}

	@Test
	public void modelShouldNotifyConnectionRemovedInRegistry() throws InterruptedException, MalformedURLException {
		// given
		IElementListener listener = spy(new VoidElementListener());
		model.addListener(listener);
		// when
		registry.remove(connection);
		// then
		verify(listener, timeout(10 * 1000)).elementChanged(any());
	}

	public class VoidElementListener implements IElementListener {

		@Override
		public void elementChanged(IOpenshiftUIElement<?, ?, ?> element) {
		}
	}

}
