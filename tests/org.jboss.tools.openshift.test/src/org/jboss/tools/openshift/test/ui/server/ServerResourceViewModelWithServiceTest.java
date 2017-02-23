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
package org.jboss.tools.openshift.test.ui.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.server.ServerResourceViewModel;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerResourceViewModelWithServiceTest {

	private ServerResourceViewModel model;
	private IService selectedService;
	private Connection connection;

	@Before
	public void setUp() {
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.model = new ServerResourceViewModel(this.selectedService = ResourceMocks.PROJECT2_SERVICES[1], connection);
		model.loadResources();
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void shouldReturnConnection() {
		// given
		// when
		IConnection connection = model.getConnection();
		// then
		assertThat(connection).isEqualTo(this.connection);
	}

	@Test
	public void shouldNotLoadResourcesBeforeBeingToldTo() {
		// given
		Connection connection = ResourceMocks.createConnection("http://10.1.2.2:8443", "dev@paas.redhat.com");
		new ServerResourceViewModel(connection);
		// when
		// then
		verify(connection, never()).getResources(any());
		verify(connection, never()).getResources(any(), any());
	}

	@Test
	public void shouldLoadResourcesWhenToldTo() {
		// given
		Connection connection = ResourceMocks.createConnection("http://10.1.2.2:8443", "dev@paas.redhat.com");
		ServerResourceViewModel model = new ServerResourceViewModel(connection);
		model.loadResources();
		// when
		// then
		verify(connection, atLeastOnce()).getResources(any());
	}

	@Test
	public void shouldReturnNullServiceIfNoConnectionIsSet() {
		// given
		ServerResourceViewModel model = new ServerResourceViewModel(null);
		model.loadResources();
		// when
		IResource resource = model.getResource();
		// then
		assertThat(resource).isNull();
	}

	@Test
	public void shouldSetNewConnectionIfLoadingWithConnection() {
		// given
		ServerResourceViewModel model = new ServerResourceViewModel(null);
		model.loadResources();
		assertThat(model.getConnection()).isNull();
		// when
		model.loadResources(connection);
		// then
		assertThat(model.getConnection()).isEqualTo(connection);
	}

	@Test
	public void shouldReturnNullResourceIfResourcesNotLoaded() {
		// given
		ServerResourceViewModel model = new ServerResourceViewModel(null);
		// when
		IResource resource = model.getResource();
		// then
		assertThat(resource).isNull();
	}

	@Test
	public void shouldReturnResourceIfResourcesAreLoaded() {
		// given
		model.loadResources();
		// when
		IResource resource = model.getResource();
		// then
		assertThat(resource).isEqualTo(selectedService);
	}

	@Test
	public void shouldReturn1stServiceInListIfInitializedServiceIsNotInListOfAllServices() {
		// given
		IService otherService = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE);
		ServerResourceViewModel model = new ServerResourceViewModel(otherService, connection);
		model.loadResources();
		// when
		IResource service = model.getResource();
		// then
		assertThat(service).isEqualTo(ResourceMocks.PROJECT2_SERVICES[0]);
	}

	@Test
	public void shouldReturnServiceForGivenName() {
		// given
		// when
		IService service = model.getService(ResourceMocks.PROJECT2_SERVICES[2].getName());
		// then
		assertThat(service).isEqualTo(ResourceMocks.PROJECT2_SERVICES[2]);
	}

	@Test
	public void shouldSetNewConnectionIfLoadResourcesWithConnection() {
		// given
		ServerResourceViewModel model = new ServerResourceViewModel(null);
		model.loadResources();
		assertThat(model.getConnection()).isNull();
		// when
		model.loadResources(connection);
		// then
		assertThat(model.getConnection()).isEqualTo(connection);
	}

	@Test
	public void shouldReturnNewServiceItemsIfLoadResourcesWithConnection() {
		// given
		List<ObservableTreeItem> serviceItems = new ArrayList<>(model.getResourceItems());

		Connection connection = ResourceMocks.createConnection("http://localhost:8080", "dev@42.org");
		IProject project = ResourceMocks.createResource(IProject.class, ResourceKind.PROJECT);
		when(connection.getResources(ResourceKind.PROJECT))
				.thenReturn(Collections.singletonList(project));
		IService service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE);
		when(project.getResources(ResourceKind.SERVICE))
				.thenReturn(Collections.singletonList(service));

		// when
		model.loadResources(connection);

		// then
		List<ObservableTreeItem> newServiceItems = model.getResourceItems();
		assertThat(newServiceItems).isNotEqualTo(serviceItems);
	}

	@Test
	public void shouldReturnNewSelectedServiceIfLoadResourcesWithConnection() {
		// given
		IResource selectedService = model.getResource();

		Connection connection = ResourceMocks.createConnection("https://localhost:8181", "ops@42.org");
		ConnectionsRegistrySingleton.getInstance().add(connection);

		try {
			IProject project = ResourceMocks.createResource(IProject.class, ResourceKind.PROJECT);
			when(connection.getResources(ResourceKind.PROJECT))
					.thenReturn(Collections.singletonList(project));
			IService service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE);
			when(project.getResources(ResourceKind.SERVICE))
					.thenReturn(Collections.singletonList(service));

			// when
			model.loadResources(connection);

			// then
			IResource newSelectedService = model.getResource();
			assertThat(selectedService).isNotEqualTo(newSelectedService);
		} finally {
			ConnectionsRegistrySingleton.getInstance().remove(connection);
		}

	}

}
