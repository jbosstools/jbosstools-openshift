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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.model.IResource;

/**
 * @author Fred Bricon
 * @author Rob Stryker
 * @author Andre Dietisheim
 *
 */
public class OpenShiftServerUtilsTest {

	private IServer server;
	private Connection connection;
	private IServerWorkingCopy serverWorkingCopy;

	@Before
	public void setUp() throws UnsupportedEncodingException, MalformedURLException {
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);

		this.serverWorkingCopy = OpenShiftServerTestUtils.mockServerWorkingCopy();
		this.server = OpenShiftServerTestUtils.mockServer(serverWorkingCopy, ResourceMocks.PROJECT2_SERVICES[1],
				connection);

	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void testGetPodPathFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_POD_PATH, (String) null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getPodPath(server));
	}

	@Test
	public void testGetSourceFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_SOURCE_PATH, (String) null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getSourcePath(server));
	}

	@Test
	public void testGetRouteURLFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_ROUTE, (String) null)).thenReturn("test1");
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
	public void should_not_return_connection_from_server_given_malformed_url() {
		// given
		doReturn("htt:/bogus").when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_CONNECTIONURL), anyString());
		// when
		Connection connection = OpenShiftServerUtils.getConnection(server);
		// then
		assertThat(connection).isNull();
	}

	@Test
	public void should_not_return_connection_from_server_given_non_registered_connection() {
		// given
		ConnectionsRegistrySingleton.getInstance().clear();
		// when
		Connection connection = OpenShiftServerUtils.getConnection(server);
		// then
		assertThat(connection).isNull();
	}

	@Test(expected = CoreException.class)
	public void should_throw_exception_given_no_connection() throws Exception {
		// given
		ConnectionsRegistrySingleton.getInstance().clear();
		// when
		OpenShiftServerUtils.getConnectionChecked(server);
		// then
		fail("CoreException expected");
	}

	@Test
	public void should_return_service_from_server() {
		// given
		// when
		IResource resource = OpenShiftServerUtils.getResource(server, connection, new NullProgressMonitor());
		// then
		assertThat(resource).isEqualTo(ResourceMocks.PROJECT2_SERVICES[1]);
	}

	@Test
	public void shouldNotShowJmxOnNonJavaProject() throws CoreException {
		IProject eclipseProject = ResourceMocks.createEclipseProject("project1");
		doReturn(eclipseProject.getName()).when(server).getAttribute(eq(OpenShiftServerUtils.ATTR_DEPLOYPROJECT),
				anyString());
		assertThat(OpenShiftServerUtils.isJavaProject(server)).isFalse();
	}

	@Test
	public void shouldFindServerGivenListOfServersContainsServerWithNullType() throws CoreException {
		// given
		IServer os3Server = mockOS3Server("os3", "someService");
		IServer[] servers = new IServer[] { mockServer("null", null), os3Server,
				mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70)) };
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
		IServer[] servers = new IServer[] { mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70)),
				os3Server1, os3Server2 };
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
		IServer[] servers = new IServer[] { mockServer("null", null), os3Server2, os3Server1,
				mockServer("as7", mockServerType(IJBossToolingConstants.SERVER_AS_70)) };
		// when
		IServer serverFound = OpenShiftServerUtils.findServerForResource("someService", servers);
		// then
		assertThat(serverFound).isSameAs(os3Server1);
	}

	@Test
	public void shouldSaveServerAttribute() throws CoreException {
		// given
		// when
		OpenShiftServerUtils.updateServerAttribute("anAttribute", "42", server);
		// then
		verify(serverWorkingCopy, atLeastOnce()).setAttribute("anAttribute", "42");
		verify(serverWorkingCopy, atLeastOnce()).save(anyBoolean(), any(IProgressMonitor.class));
	}

	@Test(expected = CoreException.class)
	public void shouldThrowExceptionIfSavingEmptyAttributeValue() throws CoreException {
		// given
		// when
		OpenShiftServerUtils.updateServerAttribute("", "42", server);
		// then
	}

	@Test(expected = CoreException.class)
	public void shouldThrowExceptionIfSavingNullAttributeValue() throws CoreException {
		// given
		// when
		OpenShiftServerUtils.updateServerAttribute(null, "42", server);
		// then
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
