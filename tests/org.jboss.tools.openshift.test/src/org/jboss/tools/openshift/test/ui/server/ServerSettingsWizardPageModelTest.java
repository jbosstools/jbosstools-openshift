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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
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
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizardPageModel;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Andre Dietisheim
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSettingsWizardPageModelTest {
	
	private ServerSettingsWizardPageModel model;
	private Connection connection;
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IProject project4;

	@Before
	public void setUp() throws CoreException {
		this.connection = ResourceMocks.createServerSettingsWizardPageConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);

		this.project1 = mockProject("project1");
		this.project2 = mockGitSharedProject("project2", ResourceMocks.PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI);
		this.project3 = mockProject("project3");
		this.project4 = mockGitSharedProject("project4", "git@42.git");

		this.model = createModel(Arrays.asList(project1, project2, project3, project4));
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void shouldReturn1stProjectIfNoProjecthasGitRemoteMatchingBuildConfig() {
		// given
		ServerSettingsWizardPageModel model = createModel(Arrays.asList(project1, project3, project4));
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project1);
	}

	@Test
	public void shouldReturnProjectThatHasGitRemoteMatchingBuildConfig() {
		// given
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project2);
	}

	private ServerSettingsWizardPageModel createModel(List<IProject> projects) {
		TestableServerSettingsWizardPageModel model = spy(new TestableServerSettingsWizardPageModel());
		doReturn(projects).when(model).loadProjects();
		model.loadResources();
		return model;
	}

	public class TestableServerSettingsWizardPageModel extends ServerSettingsWizardPageModel {

		public TestableServerSettingsWizardPageModel() {
			super(ResourceMocks.PROJECT2_SERVICES[1], null, null, connection, null);
		}

		@Override
		public List<org.eclipse.core.resources.IProject> loadProjects() {
			return super.loadProjects();
		}
	}

	private IProject mockProject(String name) throws CoreException {
		IProject project = mock(IProject.class);
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

	private IProject mockGitSharedProject(String name, String gitRemoteUri) throws CoreException {
		IProject project = mockProject(name);

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
