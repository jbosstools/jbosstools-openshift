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
package org.jboss.tools.openshift.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.mockito.Mockito;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class ResourceMocks {
	
	public static final IProject[] PROJECTS = new IProject[] {
			createResource(IProject.class, 
					project -> when(project.getName()).thenReturn("project1")),
			createResource(IProject.class, 
					project -> when(project.getName()).thenReturn("project2")),
			createResource(IProject.class, 
					project -> when(project.getName()).thenReturn("project3"))
	};

	public static final IService[] PROJECT2_SERVICES = new IService[] {
			createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-app1")),
			createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-app2")),
			createResource(IService.class,
					service -> when(service.getName()).thenReturn("project2-app3"))
	};

	public static final IBuildConfig[] PROJECT2_BUILDCONFIGS = new IBuildConfig[] {
			createResource(IBuildConfig.class,
					// needs to match service name
					service -> when(service.getName()).thenReturn("project2-app2")),
			createResource(IBuildConfig.class,
					// needs to match service name
					service -> when(service.getName()).thenReturn("project2-app3"))
	};

	public static final IRoute[] PROJECT2_ROUTES = new IRoute[] {
			createResource(IRoute.class,
					service -> when(service.getName()).thenReturn("project3-app2")),
			createResource(IRoute.class,
					service -> when(service.getName()).thenReturn("project3-app3"))
	};

	public static final IService[] PROJECT3_SERVICES = new IService[] {
			createResource(IService.class,
					service -> when(service.getName()).thenReturn("project3-app1")),
			createResource(IService.class,
					service -> when(service.getName()).thenReturn("project3-app2"))
	};

	public static Connection createServerSettingsWizardPageConnection() {
		Connection connection = createConnection("http://localhost:8443", "dev@openshift.com");
		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(PROJECTS));
		when(PROJECTS[1].getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT2_SERVICES));
		when(PROJECTS[2].getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT3_SERVICES));
		when(connection.getResources(ResourceKind.ROUTE, PROJECTS[1].getName())).thenReturn(Arrays.asList(PROJECT2_BUILDCONFIGS));
		return connection;
	}


	public static Connection createConnection(String host, String username) {
		Connection connection = mock(Connection.class);
		when(connection.getHost()).thenReturn(host);
		when(connection.getUsername()).thenReturn(username);
		when(connection.isDefaultHost()).thenReturn(false);
		return connection;
	}

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz) {
		return createResources(numOf, clazz, null);
	}

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz, IResourceVisitor<R> visitor) {
		List<R> resources = new ArrayList<>(numOf);
		
		for (int i = 0; i< numOf; i++) {
			R mock = createResource(clazz, visitor);
			resources.add(mock);
		}
		return resources;
	}

	public static <R extends IResource> R createResource(Class<R> clazz) {
		return createResource(clazz, null);
	}

	public static <R extends IResource> R createResource(Class<R> clazz, IResourceVisitor<R> visitor) {
		R mock = Mockito.mock(clazz);
		if (visitor != null) {
			visitor.visit(mock);
		}
		return mock;
	}

	public static List<ObservableTreeItem> createObservableTreeItems(Collection<? extends IResource> resources) {
		return resources.stream()
				.map(r -> new ObservableTreeItem(r))
				.collect(Collectors.toList());
	}
	
	public static interface IResourceVisitor<R extends IResource> {
		public void visit(R resource);
	}
}
