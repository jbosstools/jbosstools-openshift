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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizardPageModel;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.build.ISourceBuildStrategy;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
@SuppressWarnings({ "restriction", "deprecation" })
@RunWith(MockitoJUnitRunner.Silent.class)
public class ServerSettingsWizardPageModelTest {

	private TestableServerSettingsWizardPageModel model;
	private Connection connection;
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IProject project4;
	@Mock
	private ServerWorkingCopy server;

	@Before
	public void setUp() throws CoreException {
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);

		this.project1 = ResourceMocks.createEclipseProject("project1");
		this.project2 = ResourceMocks.createGitSharedProject("project2",
				ResourceMocks.PROJECT2_BUILDCONFIG2_BUILD_SOURCEURI);
		this.project3 = ResourceMocks.createEclipseProject("project3");
		this.project4 = ResourceMocks.createGitSharedProject("project4", "git@42.git");

		this.model = createModel(ResourceMocks.PROJECT2_SERVICES[1], null, null,
				Arrays.asList(project1, project2, project3, project4), connection, server);
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void shouldReturn1stProjectIfNoMatchesGitRemoteOfServiceModelWasInitializedWith() {
		// given
		ServerSettingsWizardPageModel model = createModel(ResourceMocks.PROJECT2_SERVICES[1], null, null,
				Arrays.asList(project1, project3, project4), connection);
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project1);
	}

	@Test
	public void shouldReturnProjectMatchingGitRemoteOfServiceModelWasInitializedWith() {
		// given
		// model initialized with resource
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(project2);
	}

	@Test
	public void shouldReturnServiceMatchingGitRemoteOfProjectModelWasInitializedWith() {
		// given
		// model initialized with project
		ServerSettingsWizardPageModel model = createModel(null, null, project2,
				Arrays.asList(project1, project2, project3, project4), connection);
		// when
		IResource resource = model.getResource();
		IProject deployProject = model.getDeployProject();
		// then
		assertThat(resource).isEqualTo(ResourceMocks.PROJECT2_SERVICES[1]);
		assertThat(deployProject).isEqualTo(project2);
	}

	@Test
	public void shouldReturn1stAvailableProjectIfModelInitializingProjectIsNotContainedInAvailableProjects() {
		// given
		ServerSettingsWizardPageModel model = createModel(null, null, project2,
				Arrays.asList(project1, project3, project4), connection);
		// when
		IProject deployProject = model.getDeployProject();
		// then
		assertThat(deployProject).isEqualTo(project1);
	}

	@Test
	public void shouldReturn1stServiceIfNoServiceMatchesGitRemoteOfProjectModelWasInitializedWith() {
		// given
		ServerSettingsWizardPageModel model = createModel(null, null, project1,
				Arrays.asList(project1, project2, project3, project4), connection);
		// when
		IResource resource = model.getResource();
		// then
		assertThat(resource).isEqualTo(ResourceMocks.PROJECT2_SERVICES[0]);
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
		ServerSettingsWizardPageModel model = createModel(ResourceMocks.PROJECT2_SERVICES[1], null, null,
				Arrays.asList(project2, project3, project4), connection);
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
		assertThat(model.isUseInferredPodPath()).isFalse();

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
	public void shouldUseDevmodeKeyThatIsSet() {
		// given
		model.setUseImageDevmodeKey(false);
		//when
		model.setDevmodeKey("gargamel");
		// then
		assertThat(model.isUseImageDevmodeKey()).isFalse();
		assertThat(model.getDevmodeKey()).isEqualTo("gargamel");
	}

	@Test
	public void shouldUseImageDevmodeKey() {
		// given
		model.setUseImageDevmodeKey(true);
		//when
		model.setDevmodeKey("gargamel");
		// then
		assertThat(model.isUseImageDevmodeKey()).isTrue();
		assertThat(model.getDevmodeKey()).isNull();
	}

	@Test
	public void shouldUseDebugPortKeyThatIsSet() {
		// given
		model.setUseImageDebugPortKey(false);
		//when
		model.setDebugPortKey("bugging-the-bugs");
		// then
		assertThat(model.isUseImageDebugPortKey()).isFalse();
		assertThat(model.getDebugPortKey()).isEqualTo("bugging-the-bugs");
	}

	@Test
	public void shouldUseImageDebugPortKey() {
		// given
		model.setUseImageDebugPortKey(true);
		//when
		model.setDebugPortKey("bugging-the-bugs");
		// then
		assertThat(model.isUseImageDebugPortKey()).isTrue();
		assertThat(model.getDebugPortKey()).isNull();
	}

	@Test
	public void shouldUseDebugPortValueThatIsSet() {
		// given
		model.setUseImageDebugPortValue(false);
		//when
		model.setDebugPortValue("42");
		// then
		assertThat(model.isUseImageDebugPortValue()).isFalse();
		assertThat(model.getDebugPortValue()).isEqualTo("42");
	}

	@Test
	public void shouldUseImageDebugPortValue() {
		// given
		model.setUseImageDebugPortValue(true);
		//when
		model.setDebugPortValue("42");
		// then
		assertThat(model.isUseImageDebugPortValue()).isTrue();
		assertThat(model.getDebugPortValue()).isNull();
	}

	@Test
	public void shouldUpdateSourcePathIfNewDeployProjectIsSet() {
		// given
		assertThat(model.getSourcePath())
				.isEqualTo(VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
		//when
		model.setDeployProject(project4);
		// then
		assertThat(model.getSourcePath())
				.isEqualTo(VariablesHelper.addWorkspacePrefix(project4.getFullPath().toString()));
	}

	@Test
	public void shouldNotUpdateSourcePathIfNewDeployProjectIsNotAccessible() {
		// given
		assertThat(model.getSourcePath())
				.isEqualTo(VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
		doReturn(false).when(project4).isAccessible();
		//when
		model.setDeployProject(project4);
		// then
		assertThat(model.getSourcePath())
				.isEqualTo(VariablesHelper.addWorkspacePrefix(project2.getFullPath().toString()));
	}

	@Test
	public void shouldReturn2RoutesThatMatchService() {
		// given
		// when
		List<IRoute> routes = model.getRoutes();
		// then
		assertThat(routes).containsOnly(ResourceMocks.PROJECT2_ROUTES[1], ResourceMocks.PROJECT2_ROUTES[2]);
	}

	@Test
	public void shouldUpdateRoutesWhenServiceIsSwitched() {
		// given
		// when
		model.setResource(ResourceMocks.PROJECT3_SERVICES[1]);
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
		model.setResource(ResourceMocks.PROJECT3_SERVICES[1]);
		// then
		assertThat(notifiedRoutes).containsOnly(ResourceMocks.PROJECT3_ROUTES[1]);
	}

	@Test
	public void should_return_route_that_was_set_if_route_points_to_correct_service() {
		// given
		model.setResource(ResourceMocks.PROJECT2_SERVICES[1]);
		// when
		model.setRoute(ResourceMocks.PROJECT2_ROUTES[1]);
		// then
		assertThat(model.getRoute()).isEqualTo(ResourceMocks.PROJECT2_ROUTES[1]);

		// given
		model.setResource(ResourceMocks.PROJECT2_SERVICES[1]);
		// when
		model.setRoute(ResourceMocks.PROJECT2_ROUTES[2]);
		// then
		assertThat(model.getRoute()).isEqualTo(ResourceMocks.PROJECT2_ROUTES[2]);
	}

	@Test
	public void should_return_1st_valid_route_if_invalid_route_is_set() {
		// given
		model.setResource(ResourceMocks.PROJECT2_SERVICES[1]);
		// when setting route that is not pointing to selected resource
		model.setRoute(ResourceMocks.PROJECT2_ROUTES[0]);
		// then 1st valid route is used
		assertThat(model.getRoute()).isEqualTo(ResourceMocks.PROJECT2_ROUTES[1]);
	}

	@Test
	public void should_return_valid_route_if_initialized_with_invalid_route() {
		// given
		ServerSettingsWizardPageModel model = createModel(ResourceMocks.PROJECT2_SERVICES[1],
				ResourceMocks.PROJECT3_ROUTES[0], null, Arrays.asList(project1, project3, project4), connection);
		// when
		IRoute route = model.getRoute();
		//then
		assertThat(route).isEqualTo(ResourceMocks.PROJECT2_ROUTES[1]);
	}

	@Test
	public void should_return_invalidOCBinary_as_it_was_set() {
		// given
		// when
		model.setOCBinaryStatus(Status.OK_STATUS);
		//then
		assertThat(model.getOCBinaryStatus()).isEqualTo(Status.OK_STATUS);

		// given
		// when
		model.setOCBinaryStatus(Status.CANCEL_STATUS);
		//then
		assertThat(model.getOCBinaryStatus()).isEqualTo(Status.CANCEL_STATUS);
	}

	@Test
	public void should_set_server_name_when_updating_server() throws CoreException {
		// given
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setName(anyString());
	}

	@Test
	public void should_set_server_host_when_updating_server() throws CoreException {
		// given
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setHost(anyString());
	}

	@Test
	public void should_set_server_connection_when_updating_server()
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		assertThat(model.getConnection()).isNotNull();
		IConnection connection = model.getConnection();
		String connectionURL = ConnectionURL.forConnection(connection).getUrl();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(OpenShiftServerUtils.ATTR_CONNECTIONURL, connectionURL);
		verify(model, atLeastOnce()).updateServerProject(eq(connectionURL),
				any(com.openshift.restclient.model.IResource.class), anyString(), any(), anyString(), any(),
				any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_deployProject_when_updating_server() throws CoreException {
		// given
		IProject deployProject = model.getDeployProject();
		assertThat(deployProject).isNotNull();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(OpenShiftServerUtils.ATTR_DEPLOYPROJECT, deployProject.getName());
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), anyString(), any(),
				anyString(), any(), any(), any(), eq(deployProject));
	}

	@Test
	public void should_set_server_sourcePath_when_updating_server() throws CoreException {
		// given
		String sourcePath = model.getSourcePath();
		assertThat(sourcePath).isNotNull();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_SOURCE_PATH), eq(sourcePath));
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), eq(sourcePath), any(),
				anyString(), any(), any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_podPath_when_updating_server() throws CoreException {
		// given
		model.setUseInferredPodPath(false);
		String podPath = "42";
		model.setPodPath(podPath);
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_POD_PATH), anyString());
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), anyString(), any(),
				eq(podPath), any(), any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_podPath_to_empty_string_when_updating_server_if_set_to_infer_it()
			throws CoreException {
		// given
		model.setUseInferredPodPath(true);
		String podPath = "42";
		model.setPodPath(podPath);
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_POD_PATH), eq(""));
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), anyString(), any(), eq(""),
				any(), any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_service_when_updating_server() throws CoreException {
		// given
		model.setSelectDefaultRoute(true);
		IResource resource = model.getResource();
		assertThat(resource).isNotNull();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		verify(model, atLeastOnce()).updateServerProject(anyString(), eq(resource), anyString(), anyString(), anyString(),
				any(), any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_route_when_updating_server() throws CoreException {
		// given
		model.setSelectDefaultRoute(true);
		IRoute route = model.getRoute();
		assertThat(route).isNotNull();
		String routeURL = route.getURL();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_ROUTE), eq(routeURL));
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), anyString(), eq(routeURL),
				anyString(), any(), any(), any(), any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_not_set_route_if_setSelecteDefaultRoute_is_false_when_updating_server() throws CoreException {
		// given
		model.setSelectDefaultRoute(false);
		IRoute route = model.getRoute();
		assertThat(route).isNotNull();
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_ROUTE), nullable(String.class));
		verify(model, atLeastOnce()).updateServerProject(anyString(), any(IService.class), anyString(), any(),
				anyString(), any(), any(), nullable(String.class),
				any(org.eclipse.core.resources.IProject.class));
	}

	@Test
	public void should_set_server_mode_when_updating_server() throws CoreException {
		// given
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(IDeployableServer.SERVER_MODE, OpenShiftServer.OPENSHIFT3_MODE_ID);
	}

	@Test
	public void should_set_autopublish_when_updating_server() throws CoreException {
		// given
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAutoPublishSetting(Server.AUTO_PUBLISH_RESOURCE);
	}

	@Test
	public void testGetStandartOpenshiftProfile() {
		assertEquals("openshift3", this.model.getProfileId());
	}

	@Test
	public void testGetNodejsOpenshiftProfile() {
		org.eclipse.core.resources.IFile appJsonFile = mock(org.eclipse.core.resources.IFile.class);
		when(appJsonFile.exists()).thenReturn(true);
		when(project2.getFile(eq("app.json"))).thenReturn(appJsonFile);

		assertEquals("openshift3.nodejs", this.model.getProfileId());
	}

	@Test
	public void testGetEapOpenshiftProfile() {
		DockerImageURI image = mock(DockerImageURI.class);
		when(image.getName()).thenReturn("super-puper-eap-name");
		ISourceBuildStrategy buildStrategy = mock(ISourceBuildStrategy.class);
		when(buildStrategy.getImage()).thenReturn(image);
		IBuildConfig buildConfig = (IBuildConfig) connection
				//take buildconfig "project2-app2" from ResourceMocks
				.getResources(ResourceKind.BUILD_CONFIG, ResourceMocks.PROJECT2.getName()).get(1);
		when(buildConfig.getBuildStrategy()).thenReturn(buildStrategy);

		assertEquals("openshift3.eap", this.model.getProfileId());
	}

	@Test
	public void shouldInitializeUseImageDevmodeKeyToFalseIfKeyIsPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		doReturn("aDevmodeKey").when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEVMODE_KEY), anyString());

		// when
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// then
		assertThat(model.isUseImageDevmodeKey()).isFalse();
	}

	@Test
	public void shouldInitializeUseImageDevmodeKeyToTrueIfKeyIsNotPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// when
		doReturn(true).when(model).isUseImageDevmodeKey();
		// then
		assertThat(model.isUseImageDevmodeKey()).isTrue();
	}

	@Test
	public void shouldInitializeDevmodeKey() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// when
		doReturn("aDevmodeKey").when(model).getDevmodeKey();
		// then
		assertThat(model.getDevmodeKey()).isEqualTo("aDevmodeKey");
	}

	@Test
	public void shouldInitializeUseImageDebugPortKeyToFalseIfKeyIsPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		doReturn("debugPortKey").when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY), anyString());

		// when
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// then
		assertThat(model.isUseImageDebugPortKey()).isFalse();
	}

	@Test
	public void shouldInitializeUseImageDebugPortKeyToTrueIfKeyIsNotPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);

		// when
		doReturn(true).when(model).isUseImageDebugPortKey();
		// then
		assertThat(model.isUseImageDebugPortKey()).isTrue();
	}

	@Test
	public void shouldInitializeDebugPortKey() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);

		// when
		doReturn("aDebugPortKey").when(model).getDebugPortKey();
		// then
		assertThat(model.getDebugPortKey()).isEqualTo("aDebugPortKey");
	}

	@Test
	public void shouldUpdateServerDebugPortKeyGivenIsUseImagePortKeyIsFalse() throws Exception {
		// given
		model.setUseImageDebugPortKey(false);
		model.setDebugPortKey("aDebugPortKey");
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY), eq("aDebugPortKey"));
	}

	@Test
	public void shouldUpdateServerDebugPortKeyToNullGivenIsUseImagePortKeyIsTrue() throws Exception {
		// given
		model.setUseImageDebugPortKey(true);
		model.setDebugPortKey("aDebugKey");
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY), eq((String) null));
	}

	@Test
	public void shouldInitializeUseImageDebugPortValueToFalseIfKeyIsPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		doReturn("debugPortValue").when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE),
				anyString());

		// when
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// then
		assertThat(model.isUseImageDebugPortValue()).isFalse();
	}

	@Test
	public void shouldInitializeUseImageDebugPortValueToTrueIfKeyIsNotPresent() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// when
		doReturn(true).when(model).isUseImageDebugPortValue();
		// then
		assertThat(model.isUseImageDebugPortValue()).isTrue();
	}

	@Test
	public void shouldInitializeDebugPortValue() throws Exception {
		// given
		IServerWorkingCopy server = spy(
				OpenShiftServerTestUtils.createOpenshift3ServerWorkingCopy("aServer", null, null, null));
		ServerSettingsWizardPageModel model = createModel(null, null, null, null, null, server);
		// when
		doReturn("4242").when(model).getDebugPortValue();
		// then
		assertThat(model.getDebugPortValue()).isEqualTo("4242");
	}

	@Test
	public void shouldUpdateServerDebugPortValueGivenIsUseImagePortValueIsFalse() throws Exception {
		// given
		model.setUseImageDebugPortValue(false);
		model.setDebugPortValue("42");
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE), eq("42"));
	}

	@Test
	public void shouldUpdateServerDebugPortValueToNullGivenIsUseImagePortValueIsTrue() throws Exception {
		// given
		model.setUseImageDebugPortValue(true);
		model.setDebugPortValue("42");
		// when
		model.updateServer();
		// then
		verify(server, atLeastOnce()).setAttribute(eq(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE), eq((String) null));
	}

	@Test
	public void shouldInitializeWithCancelOCStatus() throws Exception {
		// given
		// when
		ServerSettingsWizardPageModel model = 
				new TestableServerSettingsWizardPageModel(null, null, null, connection, server);
		// then
		assertThat(model.getOCBinaryStatus()).isNotNull();
		assertThat(model.getOCBinaryStatus().getSeverity()).isEqualTo(IStatus.CANCEL);
	}	
	
	@Test
	public void shouldUpdateOCStatusWhenLoading() throws Exception {
		// given
		TestableServerSettingsWizardPageModel model = 
				spy(new TestableServerSettingsWizardPageModel(null, null, null, connection, server));
		// when
		model.loadResources();
		// then
		verify(model).setOCBinaryStatus(any(IStatus.class));
	}	

	private ServerSettingsWizardPageModel createModel(IService service, IRoute route,
			org.eclipse.core.resources.IProject deployProject, List<IProject> projects, Connection connection) {
		return createModel(service, route, deployProject, projects, connection, null);
	}

	private TestableServerSettingsWizardPageModel createModel(IService service, IRoute route,
			org.eclipse.core.resources.IProject deployProject, List<IProject> projects, Connection connection,
			IServerWorkingCopy server) {
		TestableServerSettingsWizardPageModel model = spy(
				new TestableServerSettingsWizardPageModel(service, route, deployProject, connection, server));
		doReturn(projects).when(model).loadProjects();
		model.loadResources();
		return model;
	}

	public class TestableServerSettingsWizardPageModel extends ServerSettingsWizardPageModel {

		public TestableServerSettingsWizardPageModel(IService service, IRoute route,
				org.eclipse.core.resources.IProject deployProject, Connection connection, IServerWorkingCopy server) {
			super(service, route, deployProject, connection, server);
		}

		@Override
		public List<org.eclipse.core.resources.IProject> loadProjects() {
			return super.loadProjects();
		}

		@Override
		protected void updateServerProject(String connectionUrl, com.openshift.restclient.model.IResource resource,
				String sourcePath, String routeURL, String podPath, String devmodeKey, String debugPortKey,
				String debugPortValue, org.eclipse.core.resources.IProject deployProject) {
			//super.updateServerProject(connectionUrl, resource, sourcePath, podPath, routeURL, deployProject);
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
		}

		@Override
		public String getProfileId() {
			return super.getProfileId();
		}

		@Override
		public void setOCBinaryStatus(IStatus ocBinaryStatus) {
			super.setOCBinaryStatus(ocBinaryStatus);
		}
	}

}
