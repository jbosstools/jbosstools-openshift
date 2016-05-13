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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.team.internal.core.TeamPlugin;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.mockito.Mockito;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
@SuppressWarnings("restriction")
public class ResourceMocks {
	
	public static final IPath SERVER_PROJECT_PREFS_FILE_PATH = 
			new Path(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME)
				.append(OpenShiftServerUtils.SERVER_PROJECT_QUALIFIER)
				.addFileExtension(EclipsePreferences.PREFS_FILE_EXTENSION);
	
	public static final IProject PROJECT1 = createResource(IProject.class, 
			project -> when(project.getName()).thenReturn("project1"));
	public static final IProject PROJECT2 = createResource(IProject.class, 
			project -> when(project.getName()).thenReturn("project1"));
	public static final IProject PROJECT3 = createResource(IProject.class, 
			project -> when(project.getName()).thenReturn("project1"));

	public static final IProject[] PROJECTS = new IProject[] { PROJECT1, PROJECT2, PROJECT3 };

	public static final IService[] PROJECT2_SERVICES = new IService[] {
			createService("project2-app1", PROJECT2),
			createService("project2-app2", PROJECT2),
			createService("project2-app3", PROJECT2)
	};

	public static final String PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI = "git@gitrepo.io/somegroup/someproject.git";

	public static final IBuildConfig[] PROJECT2_BUILDCONFIGS = new IBuildConfig[] {
			// needs to match service name
			createBuildConfig("project2-app1", null, null, null),
			// needs to match service name
			createBuildConfig("project2-app2", null, null, PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI),
			// needs to match service name
			createBuildConfig("project2-app3", null, null, null),
			// needs to match service name
			createBuildConfig("project2-app4", null, null, null)
	};

	public static final IRoute[] PROJECT2_ROUTES = new IRoute[] {
			// 2nd param must match service name
			createRoute("project2-app1-route1", "project2-app1"),
			createRoute("project2-app2-route2", "project2-app2"),
			createRoute("project2-app2-route3", "project2-app2"),
			createRoute("project2-app3-route4", "project2-app3")
	};

	public static final IService[] PROJECT3_SERVICES = new IService[] {
			createService("project3-app1", PROJECT3),
			createService("project3-app2", PROJECT3),
	};

	public static final IRoute[] PROJECT3_ROUTES = new IRoute[] {
			// 2nd param must match service name
			createRoute("project3-app1-route1", "project3-app1"),
			createRoute("project3-app2-route2", "project3-app2"),
			createRoute("project3-app3-route3", "bogus")
	};

	public static Connection createServerSettingsWizardPageConnection() {
		Connection connection = createConnection("http://localhost:8443", "dev@openshift.com");
		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(PROJECTS));
		when(PROJECT2.getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT2_SERVICES));
		when(PROJECT2.getResources(ResourceKind.ROUTE)).thenReturn(Arrays.asList(PROJECT2_ROUTES));
		when(connection.getResources(ResourceKind.BUILD_CONFIG, PROJECT2.getName())).thenReturn(Arrays.asList(PROJECT2_BUILDCONFIGS));
		when(PROJECT3.getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT3_SERVICES));
		when(PROJECT3.getResources(ResourceKind.ROUTE)).thenReturn(Arrays.asList(PROJECT3_ROUTES));
		return connection;
	}


	public static Connection createConnection(String host, String username) {
		Connection connection = mock(Connection.class);
		when(connection.getHost()).thenReturn(host);
		when(connection.getUsername()).thenReturn(username);
		when(connection.isDefaultHost()).thenReturn(false);
		return connection;
	}
	
	public static IBuildConfig createBuildConfig(String buildOutputReferenceKind, String buildOutputReferenceName) {
		return createBuildConfig(null, buildOutputReferenceKind, buildOutputReferenceName, null);
	}

	public static IBuildConfig createBuildConfig(String name, String buildOutputReferenceKind, String buildOutputReferenceName, String buildSourceURI) {
		IBuildConfig bc = mock(IBuildConfig.class);
		
		IObjectReference reference = mock(IObjectReference.class);
		when(reference.getKind()).thenReturn(buildOutputReferenceKind);
		when(reference.getName()).thenReturn(buildOutputReferenceName);
		when(bc.getBuildOutputReference()).thenReturn(reference);
		
		when(bc.getName()).thenReturn(name);
		when(bc.getSourceURI()).thenReturn(buildSourceURI);

		return bc;
	}

	public static IRoute createRoute(String name, String serviceName) {
		return createResource(IRoute.class, 
				route -> {
					when(route.getName()).thenReturn(name);
					when(route.getServiceName()).thenReturn(serviceName);
					when(route.getURL()).thenReturn("http://" + serviceName);
				});
	}
	
	public static IService createService(String name, IProject project) {
		return createResource(IService.class, 
				service -> {
					when(service.getName()).thenReturn(name);
					when(service.getProject()).thenReturn(project);
				});
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

	public static org.eclipse.core.resources.IProject createProject(String name) throws CoreException {
		org.eclipse.core.resources.IProject project = mock(org.eclipse.core.resources.IProject.class);
		when(project.isAccessible()).thenReturn(true);
		when(project.getName()).thenReturn(name);
		when(project.getLocation()).thenReturn(new Path(File.separator + name));
		IPath projectFullPath = new Path(
				ResourcesPlugin.getWorkspace().getRoot().getFullPath().toString() 
						+ File.separator + name);
		when(project.getFullPath()).thenReturn(projectFullPath);
		when(project.getProject()).thenReturn(project);

		IProjectDescription description = mock(IProjectDescription.class);
		when(description.getNatureIds()).thenReturn(new String[] {});
		when(project.getDescription()).thenReturn(description);

		return project;
	}

	public static org.eclipse.core.resources.IProject mockGitSharedProject(String name, String gitRemoteUri) throws CoreException {
		org.eclipse.core.resources.IProject project = createProject(name);

		when(project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY)).thenReturn(GitProvider.ID);

		when(project.getWorkingLocation(any())).thenReturn(new Path(ResourcesPlugin.getWorkspace().getRoot().getFullPath().toString()));

		StoredConfig config = mock(StoredConfig.class);
		when(config.getSubsections("remote")).thenReturn(new HashSet<String>(Arrays.asList("origin")));
		when(config.getStringList(any(), any(), any())).thenReturn(new String[] { gitRemoteUri });
		when(config.getStringList("remote", "origin", "url")).thenReturn(new String[] { gitRemoteUri });

		Repository repository = mock(Repository.class);
		when(repository.getConfig()).thenReturn(config);
		
		RepositoryMapping mapping = mock(RepositoryMapping.class);
		when(mapping.getRepository()).thenReturn(repository);

		GitProjectData data = mock(GitProjectData.class);
		when(data.getRepositoryMapping(project)).thenReturn(mapping);
		
		GitProvider repositoryProvider = mock(GitProvider.class);
		when(repositoryProvider.getID()).thenReturn(GitProvider.ID);
		when(repositoryProvider.getData()).thenReturn(data);
		when(project.getSessionProperty(TeamPlugin.PROVIDER_PROP_KEY)).thenReturn(repositoryProvider);

		return project;
	}

}
