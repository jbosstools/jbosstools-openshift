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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.mockito.Mockito;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
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
	
	public static final IProject PROJECT1 = createProject("project1");
	public static final IProject PROJECT2 = createProject("project2");
	public static final IProject PROJECT3 = createProject("project3");
    public static final IProject PROJECT4 = createProject("project4");
    public static final IProject PROJECT5 = createProject("project5");

	public static final IProject[] PROJECTS = new IProject[] { PROJECT1, PROJECT2, PROJECT3, PROJECT4, PROJECT5 };

	public static final IService[] PROJECT2_SERVICES = new IService[] {
			// selectors need to match pod labels
			createService("project2-app1", PROJECT2, new HashMap<String, String>() {{ put("key1", "42"); put("key2", "24"); put("key3", "48"); }}),
			createService("project2-app2", PROJECT2, new HashMap<String, String>() {{ put("key1", "84"); put("key2", "48"); }}),
			createService("project2-app3", PROJECT2, new HashMap<String, String>() {{ put("key1", "42"); put("key2", "24"); }})
	};

	public static final String PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI = "git@gitrepo.io/somegroup/someproject.git";

	public static final IBuildConfig[] PROJECT2_BUILDCONFIGS = new IBuildConfig[] {
			// needs to match service name
			createBuildConfig("project2-app1", PROJECT2, null, null, null),
			// needs to match service name
			createBuildConfig("project2-app2", PROJECT2, null, null, PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI),
			// needs to match service name
			createBuildConfig("project2-app3", PROJECT2, null, null, null),
			// needs to match service name
			createBuildConfig("project2-app4", PROJECT2, null, null, null)
	};

	public static final IRoute[] PROJECT2_ROUTES = new IRoute[] {
			// 3rd param must match service name
			createRoute("project2-app1-route1", PROJECT2, "project2-app1"),
			createRoute("project2-app2-route2", PROJECT2, "project2-app2"),
			createRoute("project2-app2-route3", PROJECT2, "project2-app2"),
			createRoute("project2-app3-route4", PROJECT2, "project2-app3")
	};

	public static final IDeploymentConfig[] PROJECT2_DEPLOYMENTCONFIGS = new IDeploymentConfig[] {
			createDeploymentConfig("project2-app1-dc", PROJECT2),
			createDeploymentConfig("project2-app2-dc", PROJECT2),
			createDeploymentConfig("project2-app3-dc", PROJECT2)
	};

	public static final IPod[] PROJECT2_PODS = new IPod[] {
			// labels need to match service selectors and contain dc name
			createPod("project2-app1", PROJECT2, new HashMap<String, String>() {{ 
				put("key1", "42"); put("key2", "24"); }}),
			createPod("project2-app2", PROJECT2, new HashMap<String, String>() {{ 
				put("key1", "84"); put("key2", "48"); put(ResourceUtils.DEPLOYMENT_CONFIG_KEY, PROJECT2_DEPLOYMENTCONFIGS[2].getName());}}),
			createPod("project2-app3", PROJECT2, new HashMap<String, String>() {{ 
				put("key1", "84"); put("key2", "48"); }})
	};

	public static final IService[] PROJECT3_SERVICES = new IService[] {
			createService("project3-app1", PROJECT3),
			createService("project3-app2", PROJECT3),
			createService("project3-app2", PROJECT3),
	};

	public static final IRoute[] PROJECT3_ROUTES = new IRoute[] {
			// 3rf param must match service name
			createRoute("project3-app1-route1", PROJECT3, "project3-app1"),
			createRoute("project3-app2-route2", PROJECT3, "project3-app2"),
			createRoute("project3-app3-route3", PROJECT3, "bogus")
	};

    public static final String PROJECT4_BUILDCONFIG2_BUILD_SOURCEURI = "git@gitrepo.io/somegroup/someproject.git";

    public static final IBuildConfig[] PROJECT4_BUILDCONFIGS = new IBuildConfig[] {
            // needs to match service name
            createBuildConfig("project4-app1", PROJECT4, null, null, null),
            // needs to match service name
            createBuildConfig("project4-app2", PROJECT4, null, null, PROJECT4_BUILDCONFIG2_BUILD_SOURCEURI),
            // needs to match service name
            createBuildConfig("project4-app3", PROJECT4, null, null, null),
            // needs to match service name
            createBuildConfig("project4-app4", PROJECT4, null, null, null)
    };

    public static final IDeploymentConfig[] PROJECT4_DEPLOYMENTCONFIGS = new IDeploymentConfig[] {
            createDeploymentConfig("project2-app1-dc", PROJECT4),
            createDeploymentConfig("project2-app2-dc", PROJECT4),
            createDeploymentConfig("project2-app3-dc", PROJECT4)
    };

    public static final IPod[] PROJECT4_PODS = new IPod[] {
            // labels need to match service selectors and contain dc name
            createPod("project4-app1", PROJECT4, new HashMap<String, String>() {{ 
                put("key1", "42"); put("key2", "24"); }}),
            createPod("project4-app2", PROJECT4, new HashMap<String, String>() {{ 
                put("key1", "84"); put("key2", "48"); put(ResourceUtils.DEPLOYMENT_CONFIG_KEY, PROJECT4_DEPLOYMENTCONFIGS[2].getName());}}),
            createPod("project4-app3", PROJECT4, new HashMap<String, String>() {{ 
                put("key1", "84"); put("key2", "48"); }})
    };

    public static final IReplicationController[] PROJECT5_REPLICATINCONTROLLERS = new IReplicationController[] {
            createDeploymentConfig("project2-app1-dc", PROJECT5),
            createDeploymentConfig("project2-app2-dc", PROJECT5),
            createDeploymentConfig("project2-app3-dc", PROJECT5)
    };

    public static final IPod[] PROJECT5_PODS = new IPod[] {
            // labels need to match service selectors and contain dc name
            createPod("project5-app1", PROJECT5, new HashMap<String, String>() {{ 
                put("key1", "42"); put("key2", "24"); }}),
            createPod("project5-app2", PROJECT5, new HashMap<String, String>() {{ 
                put("key1", "84"); put("key2", "48");}}),
            createPod("project5-app3", PROJECT5, new HashMap<String, String>() {{ 
                put("key1", "84"); put("key2", "48"); }})
    };

    public static Connection create3ProjectsConnection() {
		Connection connection = createConnection("http://localhost:8443", "dev@openshift.com");
		when(connection.getResources(ResourceKind.PROJECT)).thenReturn(Arrays.asList(PROJECTS));
		when(PROJECT2.getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT2_SERVICES));
		when(connection.getResources(ResourceKind.SERVICE, PROJECT2.getName())).thenReturn(Arrays.asList(PROJECT2_SERVICES));
		when(PROJECT2.getResources(ResourceKind.ROUTE)).thenReturn(Arrays.asList(PROJECT2_ROUTES));
		when(connection.getResources(ResourceKind.BUILD_CONFIG, PROJECT2.getName())).thenReturn(Arrays.asList(PROJECT2_BUILDCONFIGS));
		when(connection.getResources(ResourceKind.POD, PROJECT2.getName())).thenReturn(Arrays.asList(PROJECT2_PODS));
		when(connection.getResources(ResourceKind.DEPLOYMENT_CONFIG, PROJECT2.getName())).thenReturn(Arrays.asList(PROJECT2_DEPLOYMENTCONFIGS));
		mockConnectionGetResource(PROJECT2_DEPLOYMENTCONFIGS, ResourceKind.DEPLOYMENT_CONFIG, connection);

		when(PROJECT3.getResources(ResourceKind.SERVICE)).thenReturn(Arrays.asList(PROJECT3_SERVICES));
		when(PROJECT3.getResources(ResourceKind.ROUTE)).thenReturn(Arrays.asList(PROJECT3_ROUTES));

	    when(connection.getResources(ResourceKind.BUILD_CONFIG, PROJECT4.getName())).thenReturn(Arrays.asList(PROJECT4_BUILDCONFIGS));
	    when(connection.getResources(ResourceKind.POD, PROJECT4.getName())).thenReturn(Arrays.asList(PROJECT4_PODS));
	    when(connection.getResources(ResourceKind.DEPLOYMENT_CONFIG, PROJECT4.getName())).thenReturn(Arrays.asList(PROJECT4_DEPLOYMENTCONFIGS));
        when(PROJECT4.getResources(ResourceKind.DEPLOYMENT_CONFIG)).thenReturn(Arrays.asList(PROJECT4_DEPLOYMENTCONFIGS));
	    mockConnectionGetResource(PROJECT4_DEPLOYMENTCONFIGS, ResourceKind.DEPLOYMENT_CONFIG, connection);

        when(connection.getResources(ResourceKind.POD, PROJECT5.getName())).thenReturn(Arrays.asList(PROJECT5_PODS));
        when(connection.getResources(ResourceKind.DEPLOYMENT_CONFIG, PROJECT5.getName())).thenReturn(Arrays.asList(PROJECT5_REPLICATINCONTROLLERS));
        when(PROJECT5.getResources(ResourceKind.REPLICATION_CONTROLLER)).thenReturn(Arrays.asList(PROJECT5_REPLICATINCONTROLLERS));
        mockConnectionGetResource(PROJECT5_REPLICATINCONTROLLERS, ResourceKind.REPLICATION_CONTROLLER, connection);

        return connection;
	}

	private static void mockConnectionGetResource(IResource[] resources, String resourceKind, Connection connection) {
		for (IResource resource : resources) {

			assertThat(resource).isNotNull();
			assertThat(resource.getName()).isNotEmpty();
			assertThat(resource.getProject()).isNotNull();
			assertThat(resource.getProject().getName()).isNotEmpty();
			assertThat(resourceKind).isNotNull();

			when(connection.getResource(resourceKind, resource.getProject().getName(), resource.getName()))
					.thenReturn(resource);
		}
	}
	
	public static IProject createProject(String name) {
		return  createResource(IProject.class, ResourceKind.PROJECT,
					project -> { 
						mockGetResourceProperties(name, project, project);
				});
	}

	public static Connection createConnection(String host, String username) {
		Connection connection = mock(Connection.class);
		when(connection.getHost()).thenReturn(host);
		when(connection.getUsername()).thenReturn(username);
		when(connection.isDefaultHost()).thenReturn(false);
		return connection;
	}
	
	public static IBuildConfig createBuildConfig(IProject project, String buildOutputReferenceKind, String buildOutputReferenceName) {
		return createBuildConfig(null, project, buildOutputReferenceKind, buildOutputReferenceName, null);
	}

	public static IBuildConfig createBuildConfig(String name, IProject project, 
			String buildOutputReferenceKind, String buildOutputReferenceName, String buildSourceURI) {
		IBuildConfig bc = mock(IBuildConfig.class);
		
		IObjectReference reference = mock(IObjectReference.class);
		when(reference.getKind()).thenReturn(buildOutputReferenceKind);
		when(reference.getName()).thenReturn(buildOutputReferenceName);
		when(bc.getBuildOutputReference()).thenReturn(reference);
		
		mockGetResourceProperties(name, project, bc);
		when(bc.getSourceURI()).thenReturn(buildSourceURI);

		return bc;
	}

	public static IRoute createRoute(String name, IProject project, String serviceName) {
		return createResource(IRoute.class, ResourceKind.ROUTE,
				route -> {
					mockGetResourceProperties(name, project, route);
					when(route.getServiceName()).thenReturn(serviceName);
					when(route.getURL()).thenReturn("http://" + serviceName);
				});
	}
	
	public static IService createService(String name, IProject project) {
		return createService(name, project, Collections.emptyMap());
	}

	public static IService createService(String name, IProject project, Map<String, String> selectors) {
		return createResource(IService.class, ResourceKind.SERVICE,
				service -> {
					mockGetResourceProperties(name, project, service);
					when(service.getSelector()).thenReturn(selectors);
				});
	}

	public static IPod createPod(String name, IProject project, Map<String, String> labels) {
		return createResource(IPod.class, ResourceKind.POD,
				pod -> {
					mockGetResourceProperties(name, project, pod);
					when(pod.getLabels()).thenReturn(labels);
				});
	}

	public static IDeploymentConfig createDeploymentConfig(String name, IProject project) {
		return createResource(IDeploymentConfig.class, ResourceKind.DEPLOYMENT_CONFIG, 
				dc -> mockGetResourceProperties(name, project, dc));
	}

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz, String kind) {
		return createResources(numOf, clazz, kind, null);
	}

	public static <R extends IResource> List<R> createResources(int numOf, Class<R> clazz, String kind, IResourceVisitor<R> visitor) {
		List<R> resources = new ArrayList<>(numOf);
		
		for (int i = 0; i< numOf; i++) {
			R mock = createResource(clazz, kind, visitor);
			resources.add(mock);
		}
		return resources;
	}

	public static <R extends IResource> R createResource(Class<R> clazz, String kind) {
		return createResource(clazz, kind, null);
	}

	public static <R extends IResource> R createResource(Class<R> clazz, String kind, IResourceVisitor<R> visitor) {
		R mock = Mockito.mock(clazz);
		when(mock.getKind()).thenReturn(kind);
		if (visitor != null) {
			visitor.visit(mock);
		}
		return mock;
	}

	public static void mockGetResourceProperties(String name, IProject project, IResource resource) {
		when(resource.getName()).thenReturn(name);
		if (project != null) {
			doReturn(project.getName()).when(resource).getNamespace();
			doReturn(project).when(resource).getProject();
		}
		
	}

	public static List<ObservableTreeItem> createObservableTreeItems(Collection<? extends IResource> resources) {
		return resources.stream()
				.map(r -> new ObservableTreeItem(r))
				.collect(Collectors.toList());
	}
	
	public static interface IResourceVisitor<R extends IResource> {
		public void visit(R resource);
	}

	public static org.eclipse.core.resources.IProject createEclipseProject(String name) throws CoreException {
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
		org.eclipse.core.resources.IProject project = createEclipseProject(name);

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
