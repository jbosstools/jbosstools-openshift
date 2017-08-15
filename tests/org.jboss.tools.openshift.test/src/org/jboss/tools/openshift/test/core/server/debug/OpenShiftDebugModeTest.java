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
package org.jboss.tools.openshift.test.core.server.debug;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IService;

public class OpenShiftDebugModeTest {
	
	private static final String KEY_DEBUGPORT = "debugPort";
	private static final String VALUE_DEBUGPORT = "42";
	private static final String KEY_DEVMODE = "DEV_MODE";
	
	private Connection connection;
	private IServer server;
	private IDeploymentConfig dc;
	private IService service;
	private TestableDebugContext context;
	private IProgressMonitor monitor;

	private Connection connection2;
	private IDeploymentConfig dc2;
	private IServer server2;
	private TestableDebugContext context2;
	private TestableDebugMode debugMode2;
	
	@Before
	public void setUp() throws CoreException, UnsupportedEncodingException, MalformedURLException {
		this.monitor = new NullProgressMonitor();
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.service = ResourceMocks.PROJECT2_SERVICES[1];
		this.dc = ResourceMocks.PROJECT2_DEPLOYMENTCONFIGS[2];
		this.server = OpenShiftServerTestUtils.mockServer(service, connection);
		this.context = new TestableDebugContext(server, KEY_DEVMODE, KEY_DEBUGPORT, VALUE_DEBUGPORT);
//		System.setProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, "2000");

		this.connection2 = ResourceMocks.createConnection("https://localhost:8181", "aUser");
		ConnectionsRegistrySingleton.getInstance().add(connection2);
		// no env var
		this.dc2 = ResourceMocks.createDeploymentConfig("someDc", ResourceMocks.createProject("someProject"), null, connection2);
		this.server2 = OpenShiftServerTestUtils.mockServer(dc2, connection2);
		this.context2 = new TestableDebugContext(server2, KEY_DEVMODE, KEY_DEBUGPORT, "42");
		this.debugMode2 = spy((TestableDebugMode) new TestableDebugMode(context2));	
	}

	@After
	public void tearDown() {
//		System.clearProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY);
		ConnectionsRegistrySingleton.getInstance().remove(connection2);
	}

	@Test
	public void debuggingContextShouldDefaultToDefaultDebugPortAndDebuggingNotEnabled() {
		// given
		// when
		DebugContext context = new DebugContext(server);
		// then
		assertThat(context.getDebugPort()).isEqualTo(NumberUtils.toInt(DebugContext.DEFAULT_DEBUG_PORT));
		assertThat(context.isDebugEnabled()).isFalse();
	}

	@Test
	public void shouldEnableDevmodeGivenItIsDisabled() throws CoreException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString())), dc2);
		// when
		context2.setDevmodeEnabled(true);
		debugMode2.execute(monitor);
		// then		
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldEnableDevmodeAndSendItGivenUserEnablesDevmodeEnvVarDoesntExist() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		context2.setDevmodeEnabled(true);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	public void shouldNotEnableDevmodeNorSendItGivenItIsAlreadyEnabled() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc2);
		// when
		context2.setDevmodeEnabled(true);
		debugMode2.execute(monitor);
		// then
		// dont alter dc
		verify(dc2, never()).setEnvironmentVariable(eq(KEY_DEVMODE), any()); 
		// dont send potentially altered dc
		verify(debugMode2, never()).sendUpdated(any(IDeploymentConfig.class), any(DebugContext.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDevmodeAndSendItGivenItIsEnabled() throws CoreException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc2);
		// when
		context2.setDevmodeEnabled(false);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugAndDevmodeGivenUserEnablesDebugAndNoEnvVarExisted() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		context2.setDebugEnabled(true);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndOnlyDevmodeIsSet() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc2);
		// when
		context2.setDebugEnabled(true);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotSetDebugGivenUserEnablesDebugAndDevmodeAndDebugEnvVarAreSet() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(
						ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()), 
						ResourceMocks.createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc2);
		// when
		context2.setDebugEnabled(true);
		debugMode2.execute(monitor);
		// then
		verify(dc2, never()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc2, never()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// dont send untouched dc
		verify(debugMode2, never()).sendUpdated(any(IDeploymentConfig.class), any(DebugContext.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndDebugEnvVarIsEnabledWithDifferentPort() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(
						ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						ResourceMocks.createEnvironmentVariable(KEY_DEBUGPORT, "84")), dc2);
		// when
		context2.setDebugEnabled(true);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc2, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabled() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(
						ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						ResourceMocks.createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc2);
		// when
		context2.setDebugEnabled(false);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc2, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabledButOnDifferentPort() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		ResourceMocks.mockEnvironmentVariables(
				Arrays.asList(
						ResourceMocks.createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						ResourceMocks.createEnvironmentVariable(KEY_DEBUGPORT, "99")), dc2);
		// when
		context2.setDebugEnabled(false);
		debugMode2.execute(monitor);
		// then
		verify(dc2, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc2, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode2, times(1)).sendUpdated(eq(dc2), eq(context2), any(IProgressMonitor.class));
	}

//	private static class MockRedeploymentJob extends Job {
//
//		private IDeploymentConfig dc;
//
//		public MockRedeploymentJob(IDeploymentConfig dc) {
//			super("MockRedeploymentJob");
//			this.dc = dc;
//			schedule(600);
//		}
//
//		@Override
//		protected IStatus run(IProgressMonitor monitor) {
//			IConnection connection = mock(IConnection.class);
//			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, mock(IDeploymentConfig.class), mock(IDeploymentConfig.class));
//			waitFor(50);
//			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, dc, dc);
//			waitFor(50);
//			IPod deployPod = mock(IPod.class);
//			when(deployPod.getName()).thenReturn("foo-deploy");
//			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, null, deployPod);
//			waitFor(50);
//			IPod pendingPod = mock(IPod.class);
//			when(pendingPod.getStatus()).thenReturn("Pending");
//			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, deployPod, pendingPod);
//			waitFor(100);
//			IPod runningPod = mock(IPod.class);
//			when(runningPod.getStatus()).thenReturn("Running");
//			when(runningPod.getName()).thenReturn("RunningPod");
//			when(runningPod.getLabels()).thenReturn(Collections.singletonMap("foo", "bar"));
//			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, pendingPod, runningPod);
//			return Status.OK_STATUS;
//		}
//
//		private void waitFor(long millis) {
//			try {
//				Thread.sleep(millis);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	public class TestableDebugMode extends OpenShiftDebugMode {

		public TestableDebugMode(DebugContext context) {
			super(context);
		}

		@Override
		public void sendUpdated(IDeploymentConfig dc, DebugContext context, IProgressMonitor monitor)
				throws CoreException {
		}
	}

	public class TestableDebugContext extends DebugContext {

		public TestableDebugContext(IServer server, String devmodeKey, String debugPortKey, String debugPort) {
			super(server, devmodeKey, debugPortKey, debugPort);
		}

		@Override
		public void setDevmodeEnabled(boolean devmodeEnabled) {
			super.setDevmodeEnabled(devmodeEnabled);
		}

		@Override
		public void setDebugEnabled(boolean debugEnabled) {
			super.setDebugEnabled(debugEnabled);
		}
	}	
}
