/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

/**
 * @author Fred Bricon
 * @author Rob Stryker
 * @author Andre Dietisheim
 *
 */
public class OpenShiftServerUtilsTest {

	private IServerWorkingCopy server;
	private Connection connection;

	@Before
	public void setUp() throws UnsupportedEncodingException, MalformedURLException {
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);

		this.server = createServer(ResourceMocks.PROJECT2_SERVICES[1]);
	}

	private IServerWorkingCopy createServer(IService serverService) throws UnsupportedEncodingException, MalformedURLException {
		IServerWorkingCopy server = mock(IServerWorkingCopy.class);
		doReturn(ConnectionURL.forConnection(connection).getUrl())
			.when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_CONNECTIONURL), anyString());
		doReturn(OpenShiftResourceUniqueId.get(serverService))
			.when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		return server;
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}
	
	@Test
	public void testIsEapStyle() {
		assertIsNotEapStyle(null);
		//docker
		assertIsNotEapStyle(createBuildConfig(IDockerBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(IDockerBuildStrategy.class, "foo.wildflybar"));
		//source
		assertIsNotEapStyle(createBuildConfig(ISourceBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ISourceBuildStrategy.class, "foo.bar.eap70"));
		//custom source
		assertIsNotEapStyle(createBuildConfig(ICustomBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ICustomBuildStrategy.class, "foo.bar.EAP64"));
		//deprecated STI
		assertIsNotEapStyle(createBuildConfig(ISTIBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ISTIBuildStrategy.class, "wildflyyy"));
		
		//fallback on template name check
		assertIsNotEapStyle(createBuildConfig(null, "foo.bar"));
		assertIsEapStyle(createBuildConfig(IBuildStrategy.class, "wildflyyy"));

	}

	@Test
	public void testContainsEap2LikeKeywords() {
		assertNotContainsEapLikeKeywords(null);
		assertNotContainsEapLikeKeywords("");
		assertContainsEapLikeKeywords("jboss-eap64");
		assertContainsEapLikeKeywords("mixed.wildFly.case");
	}

	private IBuildConfig createBuildConfig(Class<? extends IBuildStrategy> clazz, String name) {
		IBuildConfig bc = mock(IBuildConfig.class);
		DockerImageURI image = mock(DockerImageURI.class);
		when(image.getName()).thenReturn(name);
		IBuildStrategy strategy = null;
		if (clazz == null) {
			strategy = mock(ISourceBuildStrategy.class);
		} else if (IDockerBuildStrategy.class.isAssignableFrom(clazz)) {
			IDockerBuildStrategy dbs = mock(IDockerBuildStrategy.class);
			when(dbs.getBaseImage()).thenReturn(image);
			strategy = dbs;
		} else if (ICustomBuildStrategy.class.isAssignableFrom(clazz)) {
			ICustomBuildStrategy cbs = mock(ICustomBuildStrategy.class);
			when(cbs.getImage()).thenReturn(image);
			strategy = cbs;
		} else if (ISTIBuildStrategy.class.isAssignableFrom(clazz)) {
			ISTIBuildStrategy sts = mock(ISTIBuildStrategy.class);
			when(sts.getImage()).thenReturn(image);
			strategy = sts;
		}  else if (ISourceBuildStrategy.class.isAssignableFrom(clazz)) {
			ISourceBuildStrategy sbs = mock(ISourceBuildStrategy.class);
			when(sbs.getImage()).thenReturn(image);
			strategy = sbs;
		}
		when(bc.getBuildStrategy()).thenReturn(strategy);
		
		Map<String,String> labels = Collections.singletonMap("template", name);
		when(bc.getLabels()).thenReturn(labels);
		
		return bc;
	}
	
	private void assertIsEapStyle(IBuildConfig buildConfig) {
		assertTrue(OpenShiftServerUtils.isEapStyle(buildConfig));
	}

	private void assertIsNotEapStyle(IBuildConfig buildConfig) {
		assertFalse(OpenShiftServerUtils.isEapStyle(buildConfig));
	}
	
	private void assertContainsEapLikeKeywords(String text) {
		assertTrue(OpenShiftServerUtils.containsEapLikeKeywords(text));
	}
	
	private void assertNotContainsEapLikeKeywords(String text) {
		assertFalse(OpenShiftServerUtils.containsEapLikeKeywords(text));
	}
	
	@Test
	public void testGetPodPathFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_POD_PATH, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getPodPath(server));
	}

	@Test
	public void testGetSourceFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_SOURCE_PATH, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getSourcePath(server));
	}

	@Test
	public void testGetRouteURLFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_ROUTE, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getRouteURL(server));
	}
	
	@Test
	public void should_return_connection_from_server() {
		// given
		// when
		Connection connection = OpenShiftServerUtils.getConnection(server);
		// then
		assertThat(connection).isEqualTo(this.connection);
	}
	
	@Test
	public void should_return_service_from_server() {
		// given
		// when
		IResource resource = OpenShiftServerUtils.getResource(server, connection);
		// then
		assertThat(resource).isEqualTo(ResourceMocks.PROJECT2_SERVICES[1]);
	}

	@Test
	public void should_return_deploymentconfig() throws CoreException {
		// given
		// when
		IDeploymentConfig deploymentConfig = (IDeploymentConfig) OpenShiftServerUtils.getReplicationController(server);
		// then
		assertThat(deploymentConfig).isEqualTo(ResourceMocks.PROJECT2_DEPLOYMENTCONFIGS[2]);
	}

	@Test
	public void should_throw_exception_no_pods_for_service() throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		try {
			OpenShiftServerUtils.getReplicationController(
				createServer(ResourceMocks.PROJECT2_SERVICES[0]));
		// then
			fail("CoreException expected");
		} catch(CoreException e) {
			assertThat(e.getMessage().contains("not find pods"));
		}
	}

	@Test
	public void should_throw_exception_no_deployconfig_name_in_pod_labels() throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		try {
			OpenShiftServerUtils.getReplicationController(
				createServer(ResourceMocks.PROJECT2_SERVICES[2]));
		// then
			fail("CoreException expected");
		} catch(CoreException e) {
			assertThat(e.getMessage().contains("not find deployment config"));
		}
	}
	
	@Test
	public void shouldNotShowJmxOnNonJavaProject() throws CoreException {
	    IProject eclipseProject = ResourceMocks.createEclipseProject("project1");
	    doReturn(eclipseProject.getName()).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEPLOYPROJECT), anyString());
	    assertThat(OpenShiftServerUtils.isJavaProject(server)).isFalse();
	}

	@Test
	public void shouldFindServerGivenListOfServersContainsServerWithNullType() throws CoreException {
		// given
		IServer os3Server = mockOS3Server("os3", "someService");
		IServer[] servers = new IServer[] {
				mockServer("null", null),
				os3Server,
				mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70))
		};
		// when
		IServer serverFound = OpenShiftServerUtils.findServerForResource("someService", servers);
		// then
		assertThat(serverFound).isSameAs(os3Server);
	}

	@Test
	public void shouldFindTheFirstServerGivenListOfServersContainsSeveralServerWithSameService() throws CoreException {
		// given
		IServer os3Server1 = mockOS3Server("os3-1", "someService");
		IServer os3Server2 = mockOS3Server("os3-2", "someService");
		IServer[] servers = new IServer[] {
				mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70)),
				os3Server1,
				os3Server2
		};
		// when
		IServer serverFound = OpenShiftServerUtils.findServerForResource("someService", servers);
		// then
		assertThat(serverFound).isSameAs(os3Server1);
	}

	@Test
	public void shouldServerThatMatchesServiceName() throws CoreException {
		// given
		IServer os3Server1 = mockOS3Server("os3-1", "someService");
		IServer os3Server2 = mockOS3Server("os3-2", "otherService");
		IServer serviceServer = mockServer("serviceServer", mockServerType(IJBossToolingConstants.AS_71));
		doReturn("someService").when(serviceServer).getAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		IServer[] servers = new IServer[] {
				mockServer("null", null),
				os3Server2,
				os3Server1,
				mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70))
		};
		// when
		IServer serverFound = OpenShiftServerUtils.findServerForResource("someService", servers);
		// then
		assertThat(serverFound).isSameAs(os3Server1);
	}

	private static IServer mockOS3Server(String name, String serviceName) {
		IServer server = mockServer(name, OpenShiftServerUtils.getServerType());
		doReturn(serviceName).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_SERVICE), anyString());
		return server;
	}

	private static IServer mockServer(String name, IServerType type) {
		IServer server = mock(IServer.class);
		doReturn(name).when(server).getName();
		doReturn(type).when(server).getServerType();
		return server;
	}
	
	private static IServerType mockServerType(String id) {
		IServerType type = mock(IServerType.class);
		doReturn(id).when(type).getId();
		return type;
	}

}
