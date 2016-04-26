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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.server.ServiceViewModel;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServiceViewModelTest {

	private static final IProject[] PROJECTS = new IProject[] {
			ResourceMocks.createResource(IProject.class, project -> when(project.getName()).thenReturn("project1")),
			ResourceMocks.createResource(IProject.class, project -> when(project.getName()).thenReturn("project2")),
			ResourceMocks.createResource(IProject.class, project -> when(project.getName()).thenReturn("project3"))
	};

	private static final IService[] PROJECT2_SERVICES = new IService[] {
			ResourceMocks.createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-service1")),
			ResourceMocks.createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-service2")),
			ResourceMocks.createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-service3"))
	};

	private static final IService[] PROJECT3_SERVICES = new IService[] {
			ResourceMocks.createResource(IService.class,
					service -> when(service.getName()).thenReturn("project3-service1")),
			ResourceMocks.createResource(IService.class,
					service -> when(service.getName()).thenReturn("project3-service2"))
	};

	private ServiceViewModel model;
	private IService selectedService;
	private Connection connection;

	@Before
	public void setUp() {
		this.connection = mockResources(
				ResourceMocks.createConnection("http://localhost:8443", "openshift-dev@redhat.com"));
		ConnectionsRegistrySingleton.getInstance().remove(connection);	
		this.model = new ServiceViewModel(this.selectedService = PROJECT2_SERVICES[1], connection);
		model.loadResources();
	}

	private Connection mockResources(Connection connection) {
		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(PROJECTS));
		when(PROJECTS[1].getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT2_SERVICES));
		when(PROJECTS[2].getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT3_SERVICES));
		return connection;
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void shouldReturnConnection() {
		// given
		// when
		Connection connection = model.getConnection();
		// then
		assertThat(connection).isEqualTo(this.connection);
	}

	@Test
	public void shouldReturnNullServiceIfNoConnectionIsSet() {
		// given
		ServiceViewModel model = new ServiceViewModel(null);
		model.loadResources();
		// when
		IService service = model.getService();
		// then
		assertThat(service).isNull();
	}

	@Test
	public void shouldSetNewConnectionIfLoadingWithConnection() {
		// given
		ServiceViewModel model = new ServiceViewModel(null);
		model.loadResources();
		assertThat(model.getConnection()).isNull();
		// when
		model.loadResources(connection);
		// then
		assertThat(model.getConnection()).isEqualTo(connection);
	}

	@Test
	public void shouldReturnNullServiceIfResourcesNotLoaded() {
		// given
		ServiceViewModel model = new ServiceViewModel(null);
		// when
		IService service = model.getService();
		// then
		assertThat(service).isNull();
	}

	@Test
	public void shouldReturnServiceIfResourcesAreLoaded() {
		// given
		model.loadResources();
		// when
		IService service = model.getService();
		// then
		assertThat(service).isEqualTo(selectedService);
	}

	@Test
	public void shouldReturn1stServiceInListIfInitializedServiceIsNotInListOfAllServices() {
		// given
		IService otherService = ResourceMocks.createResource(IService.class);
		ServiceViewModel model = new ServiceViewModel(otherService, connection);
		model.loadResources();
		// when
		IService service = model.getService();
		// then
		assertThat(service).isEqualTo(PROJECT2_SERVICES[0]);
	}

	@Test
	public void shouldReturnServiceForGivenName() {
		// given
		// when
		IService service = model.getService(PROJECT2_SERVICES[2].getName());
		// then
		assertThat(service).isEqualTo(PROJECT2_SERVICES[2]);
	}

	@Test
	public void shouldSetNewConnectionIfLoadResourcesWithConnection() {
		// given
		ServiceViewModel model = new ServiceViewModel(null);
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
		List<ObservableTreeItem> serviceItems = new ArrayList<>(model.getServiceItems());

		Connection connection = ResourceMocks.createConnection("http://localhost:8080", "dev@42.org");
		IProject project = ResourceMocks.createResource(IProject.class);
		when(connection.getResources(ResourceKind.PROJECT))
				.thenReturn(Collections.singletonList(project));
		when(project.getResources(ResourceKind.SERVICE))
				.thenReturn(Collections.singletonList(ResourceMocks.createResource(IService.class)));

		// when
		model.loadResources(connection);

		// then
		List<ObservableTreeItem> newServiceItems = model.getServiceItems();
		assertThat(newServiceItems).isNotEqualTo(serviceItems);
	}

	@Test
	public void shouldReturnNewSelectedServiceIfLoadResourcesWithConnection() {
		// given
		IService selectedService = model.getService();

		Connection connection = ResourceMocks.createConnection("https://localhost:8181", "ops@42.org");
		ConnectionsRegistrySingleton.getInstance().add(connection);

		try {
			IProject project = ResourceMocks.createResource(IProject.class);
			when(connection.getResources(ResourceKind.PROJECT))
					.thenReturn(Collections.singletonList(project));
			IService service = ResourceMocks.createResource(IService.class);
			when(project.getResources(ResourceKind.SERVICE))
					.thenReturn(Collections.singletonList(service));

			// when
			model.loadResources(connection);

			// then
			IService newSelectedService = model.getService();
			assertThat(selectedService).isNotEqualTo(newSelectedService);
		} finally {
			ConnectionsRegistrySingleton.getInstance().remove(connection);
		}

	}

}
