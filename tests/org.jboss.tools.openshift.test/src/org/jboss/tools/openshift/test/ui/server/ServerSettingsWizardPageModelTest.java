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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizardPageModel;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

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

		this.project1 = ResourceMocks.mockProject("project1");
		this.project2 = ResourceMocks.mockGitSharedProject("project2", ResourceMocks.PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI);
		this.project3 = ResourceMocks.mockProject("project3");
		this.project4 = ResourceMocks.mockGitSharedProject("project4", "git@42.git");

		this.model = createModel(ResourceMocks.PROJECT2_SERVICES[1], null, Arrays.asList(project1, project2, project3, project4), connection);
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void shouldReturn1stProjectIfNoMatchesGitRemoteOfServiceModelWasInitializedWith() {
		// given
		ServerSettingsWizardPageModel model = 
				createModel(ResourceMocks.PROJECT2_SERVICES[1], null, Arrays.asList(project1, project3, project4), connection);
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project1);
	}

	@Test
	public void shouldReturnProjectMatchingGitRemoteOfServiceModelWasInitializedWith() {
		// given
		// model initialized with service
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project2);
	}

	@Test
	public void shouldReturnServiceMatchingGitRemoteOfProjectModeldWasInitializedWith() {
		// given
		// model initialized with project
		ServerSettingsWizardPageModel model = createModel(null, project2, Arrays.asList(project1, project2, project3, project4), connection);
		// when
		IService service = model.getService();
		IProject deployProject = model.getDeployProject();
		// then
		assertThat(service).isEqualTo(ResourceMocks.PROJECT2_SERVICES[1]);
		assertThat(deployProject).isEqualTo(project2);
	}

	@Test
	public void shouldReturn1stAvailableProjectIfModelInitializingProjectIsNotContainedInAvailableProjects() {
		// given
		ServerSettingsWizardPageModel model = createModel(null, project2, Arrays.asList(project1, project3, project4), connection);
		// when
		IProject deployProject = model.getDeployProject();
		// then
		assertThat(deployProject).isEqualTo(project1);
	}

	@Test
	public void shouldReturn1stServiceIfNoServiceMatchesGitRemoteOfProjectModelWasInitializedWith() {
		// given
		ServerSettingsWizardPageModel model = createModel(null, project1, Arrays.asList(project1, project2, project3, project4), connection);
		// when
		IService service = model.getService();
		// then
		assertThat(service).isEqualTo(ResourceMocks.PROJECT2_SERVICES[0]);
	}

	@Test
	public void shouldReturnProjectMatchingServiceByGitRemoteIfSettingNullDeployProject() {
		// given
		// when
		model.setDeployProject(null);
		// then
		// project2 matches ResourceMocks.PROJECT2_SERVICES[1] in git remote
		assertThat(model.getDeployProject()).isEqualTo(project2); 
	}

	@Test
	public void shouldReturnProjectMatchingServiceByGitRemoteIfSettingUnavailableDeployProject() {
		// given
		ServerSettingsWizardPageModel model = createModel(ResourceMocks.PROJECT2_SERVICES[1], null, Arrays.asList(project2, project3, project4), connection);
		assertThat(model.getDeployProject()).isEqualTo(project2); 
		// when
		model.setDeployProject(project1);
		// then
		// project2 matches ResourceMocks.PROJECT2_SERVICES[1] in git remote
		assertThat(model.getDeployProject()).isEqualTo(project2); 
	}

	@Test
	public void shouldRespectUseInferredPodPath() {
		// given
		//when
		model.setUseInferredPodPath(false);
		// then
		Assert.assertEquals(false, model.isUseInferredPodPath());

		// given
		//when
		model.setUseInferredPodPath(true);
		// then
		assertThat(model.isUseInferredPodPath()).isTrue();
	}

	@Test
	public void shouldRespectPodPathThatIsSet() {
		// given
		model.setUseInferredPodPath(false);
		//when
		model.setPodPath("somePath");
		// then
		assertThat(model.getPodPath()).isEqualTo("somePath");
	}

	@Test
	public void shouldUpdateSourcePathIfNewDeployProjectIsSet() {
		// given
		assertThat(model.getSourcePath()).isEqualTo(
				VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
		//when
		model.setDeployProject(project4);
		// then
		assertThat(model.getSourcePath()).isEqualTo(
				VariablesHelper.addWorkspacePrefix(project4.getFullPath().toString()));
	}

	@Test
	public void shouldNotUpdateSourcePathIfNewDeployProjectIsNotAccessible() {
		// given
		assertThat(model.getSourcePath()).isEqualTo(
				VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
		doReturn(false).when(project4).isAccessible();
		//when
		model.setDeployProject(project4);
		// then
		assertThat(model.getSourcePath()).isEqualTo(
				VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
	}

	@Test
	public void shouldReturn2RoutesThatMatchService() {
		// given
		// when
		List<IRoute> routes = model.getRoutes();
		// then
		assertThat(routes).containsOnly(
				ResourceMocks.PROJECT2_ROUTES[1], ResourceMocks.PROJECT2_ROUTES[2]);
	}

	@Test
	public void shouldUpdateRoutesWhenServiceIsSwitched() {
		// given
		// when
		model.setService(ResourceMocks.PROJECT3_SERVICES[1]);
		List<IRoute> routes = model.getRoutes();
		// then
		assertThat(routes).containsOnly(ResourceMocks.PROJECT3_ROUTES[1]);
	}

	@Test
	public void shouldNotifyRoutesMatchingSelectedService() {
		// given
		List<IRoute> notifiedRoutes = new ArrayList<>();
		model.addPropertyChangeListener(new PropertyChangeListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (ServerSettingsWizardPageModel.PROPERTY_ROUTES.equals(event.getPropertyName())) {
					assertThat(event.getNewValue()).isInstanceOf(List.class);
					notifiedRoutes.addAll((List<IRoute>) event.getNewValue());
				}
			}
		});
		// when
		model.setService(ResourceMocks.PROJECT3_SERVICES[1]);
		// then
		assertThat(notifiedRoutes).containsOnly(ResourceMocks.PROJECT3_ROUTES[1]);
	}

	private ServerSettingsWizardPageModel createModel(IService service, org.eclipse.core.resources.IProject deployProject, List<IProject> projects, Connection connection) {
		TestableServerSettingsWizardPageModel model = 
				spy(new TestableServerSettingsWizardPageModel(service, null, deployProject, connection));
		doReturn(projects).when(model).loadProjects();
		model.loadResources();
		return model;
	}

	public class TestableServerSettingsWizardPageModel extends ServerSettingsWizardPageModel {

		public TestableServerSettingsWizardPageModel(IService service, IRoute route, org.eclipse.core.resources.IProject deployProject, Connection connection) {
			super(service, route, deployProject, connection, null);
		}

		@Override
		public List<org.eclipse.core.resources.IProject> loadProjects() {
			return super.loadProjects();
		}
	}

}
