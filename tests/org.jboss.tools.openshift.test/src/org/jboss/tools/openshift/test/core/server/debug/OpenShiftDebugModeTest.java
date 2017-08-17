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

import static java.util.Arrays.asList;
import static org.apache.commons.lang.math.NumberUtils.toInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createConnection;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createDeploymentConfig;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createEnvironmentVariable;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createProject;
import static org.jboss.tools.openshift.test.util.ResourceMocks.mockGetEnvironmentVariables;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.model.IDeploymentConfig;

public class OpenShiftDebugModeTest {
	
	private static final String KEY_DEBUGPORT = "debugPort";
	private static final String VALUE_DEBUGPORT = "42";
	private static final String KEY_DEVMODE = "DEV_MODE";

	private Connection connection;
	private IDeploymentConfig dc;
	private IServer server;
	private TestableDebugContext context;
	private TestableDebugMode debugMode;
	
	@Before
	public void setUp() throws CoreException, UnsupportedEncodingException, MalformedURLException {
//		System.setProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, "2000");

		this.connection = createConnection("https://localhost:8181", "aUser");
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.dc = createDeploymentConfig(
				"someDc", 
				createProject("someProject"), 
				// no env var
				null,
				// no containers
				null,
				connection);
		this.server = OpenShiftServerTestUtils.mockServer(dc, connection);
		this.context = new TestableDebugContext(server, KEY_DEVMODE, KEY_DEBUGPORT, VALUE_DEBUGPORT);
		this.debugMode = spy((TestableDebugMode) new TestableDebugMode(context));	
	}

	@After
	public void tearDown() {
//		System.clearProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY);
		ConnectionsRegistrySingleton.getInstance().remove(connection);
	}

	@Test
	public void debuggingContextShouldDefaultToDefaultDebugPortAndDebuggingNotEnabled() {
		// given
		// when
		DebugContext context = new DebugContext(server);
		// then
		assertThat(context.getDebugPort()).isEqualTo(toInt(DebugContext.DEFAULT_DEBUG_PORT));
		assertThat(context.isDebugEnabled()).isFalse();
	}

	@Test
	public void shouldDisableContextDevmode() {
		// given
		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);
		// when
		debugMode.disableDevmode();
		// then
		assertThat(context.isDebugEnabled()).isTrue();
		assertThat(context.isDevmodeEnabled()).isFalse();
	}
	
	@Test
	public void shouldEnableContextDevmode() {
		// given
		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);
		// when
		debugMode.enableDevmode();
		// then
		assertThat(context.isDebugEnabled()).isFalse();
		assertThat(context.isDevmodeEnabled()).isTrue();
	}

	@Test
	public void shouldEnableDevmodeGivenItIsDisabled() throws CoreException {
		// given
		mockGetEnvironmentVariables(
				asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.FALSE.toString())), dc);
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then		
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldEnableDevmodeAndSendItGivenUserEnablesDevmodeEnvVarDoesntExist() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	public void shouldNotEnableDevmodeNorSendItGivenItIsAlreadyEnabled() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		// dont alter dc
		verify(dc, never()).setEnvironmentVariable(eq(KEY_DEVMODE), any()); 
		// dont send potentially altered dc
		verify(debugMode, never()).sendUpdated(any(IDeploymentConfig.class), any(DebugContext.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDevmodeAndSendItGivenItIsEnabled() throws CoreException {
		// given
		mockGetEnvironmentVariables(
				asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		// when
		context.setDevmodeEnabled(false);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableContextDebugAndDevmode() {
		// given
		context.setDebugEnabled(true);
		context.setDevmodeEnabled(true);
		// when
		debugMode.disableDebugging();
		// then
		assertThat(context.isDebugEnabled()).isFalse();
		assertThat(context.isDevmodeEnabled()).isFalse();
	}	
	
	@Test
	public void shouldEnableContextDebugAndDevmode() {
		// given
		context.setDebugEnabled(false);
		context.setDevmodeEnabled(false);
		// when
		debugMode.enableDebugging();
		// then
		assertThat(context.isDebugEnabled()).isTrue();
		assertThat(context.isDevmodeEnabled()).isTrue();
	}	

	@Test
	public void shouldSendUpdatedDebugAndDevmodeGivenUserEnablesDebugAndNoEnvVarExisted() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndOnlyDevmodeIsSet() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString())), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldNotSetDebugGivenUserEnablesDebugAndDevmodeAndDebugEnvVarAreSet() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(
						createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()), 
						createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, never()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, never()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// dont send untouched dc
		verify(debugMode, never()).sendUpdated(any(IDeploymentConfig.class), any(DebugContext.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldSendUpdatedDebugGivenUserEnablesDebugAndDebugEnvVarIsEnabledWithDifferentPort() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(
						createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						createEnvironmentVariable(KEY_DEBUGPORT, "84")), dc);
		// when
		context.setDebugEnabled(true);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString());
		verify(dc, atLeastOnce()).setEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabled() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(
						createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						createEnvironmentVariable(KEY_DEBUGPORT, VALUE_DEBUGPORT)), dc);
		// when
		context.setDebugEnabled(false);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
	}

	@Test
	public void shouldDisableDebugAndSendGivenUserDisablesDebugAndDebugEnvVarIsEnabledButOnDifferentPort() 
			throws CoreException, UnsupportedEncodingException, MalformedURLException {
		// given
		mockGetEnvironmentVariables(
				asList(
						createEnvironmentVariable(KEY_DEVMODE, Boolean.TRUE.toString()),
						createEnvironmentVariable(KEY_DEBUGPORT, "99")), dc);
		// when
		context.setDebugEnabled(false);
		debugMode.execute(new NullProgressMonitor());
		// then
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEVMODE);
		verify(dc, atLeastOnce()).removeEnvironmentVariable(KEY_DEBUGPORT);
		// send updated dc
		verify(debugMode, times(1)).sendUpdated(eq(dc), eq(context), any(IProgressMonitor.class));
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
