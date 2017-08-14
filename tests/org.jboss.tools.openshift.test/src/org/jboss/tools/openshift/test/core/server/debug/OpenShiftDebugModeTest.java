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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;
import org.jboss.tools.openshift.internal.core.util.NewPodDetectorJob;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IService;

public class OpenShiftDebugModeTest {
	
	private Connection connection;
	private IServer server;
	private IDeploymentConfig dc;
	private IService service;
	private DebugContext context;
	private IProgressMonitor monitor;

	@Before
	public void setUp() throws CoreException, UnsupportedEncodingException, MalformedURLException {
		this.monitor = new NullProgressMonitor();
		this.connection = ResourceMocks.create3ProjectsConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.service = ResourceMocks.PROJECT2_SERVICES[1];
		this.dc = ResourceMocks.PROJECT2_DEPLOYMENTCONFIGS[1];
		this.server = OpenShiftServerTestUtils.mockServer(service, connection);
		this.context = new DebugContext(server, "devmode", "debugPort", "42");
//		System.setProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, "2000");
	}

	@After
	public void tearDown() {
		System.clearProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY);
	}
	
	@Test
	public void debuggingContextShouldDefaultToDefaultDebugPortAndDebuggingNotEnabled() {
		// given
		// when
		DebugContext context = new DebugContext(server);
		// then
		assertEquals(NumberUtils.toInt(DebugContext.DEFAULT_DEBUG_PORT), context.getDebugPort());
		assertFalse(context.isDebugEnabled());
	}

	@Test
	public void shouldEnableDCDebugEnvVarGivenItIsDisabled() throws CoreException {
		// given
		TestableDebugMode debugMode = spy((TestableDebugMode) new TestableDebugMode(context));
		// when
		context.setDevmodeEnabled(true);
		debugMode.execute(monitor);
		// then
		verify(debugMode, times(1)).sendUpdated(eq(dc), any(DebugContext.class), any(IProgressMonitor.class));
	}

	@Test
	public void shouldHaveDebugEnabledGivenDevModeAndPortEnvVarsExist() {
	}

	@Test
	public void shouldHaveDebugDisabledGivenOnlyDevModeEnvVarExist() {
	}

	@Test
	public void shouldHaveDebugEnabledGivenOnlyDebugEnvVarExist() {
	}

	@Test
	public void testEnableDebug() throws CoreException {
//		DebugContext context = new DebugContext();
//		context.setDebugPort(1234);
//		IDebugListener listener = mock(IDebugListener.class);
//		context.setDebugListener(listener);
//
//		IDeploymentConfig dc = mockDeploymentConfig();
//		IClient client = mock(IClient.class);
//		when(dc.accept(any(), any())).thenReturn(client);
//		new MockRedeploymentJob(dc);
//		context.enableDebugging(dc, context, monitor);
//
//		verify(dc).setEnvironmentVariable("DEBUG_PORT", "1234");
//		verify(dc).setEnvironmentVariable("DEBUG", "true");
//		verify(listener).onPodRestart(isA(DebugContext.class), eq(monitor));
//		verify(client).update(dc);
//		IPod pod = context.getPod();
//		assertNotNull(pod);
//		assertNotNull("RunningPod", pod.getName());
	}

	@Test
	public void testAlreadyDebugging() throws CoreException {
//		DebugContext context = new DebugContext();
//		context.setDebugPort(1234);
//		context.setDebugEnabled(true);
//		IDebugListener listener = mock(IDebugListener.class);
//		context.setDebugListener(listener);
//
//		IDeploymentConfig dc = mockDeploymentConfig();
//
//		context.enableDebugging(dc, context, monitor);
//
//		verify(listener).onDebugChange(isA(DebugContext.class), eq(monitor));
	}

	@Test
	public void testDisableDebug() throws CoreException {
//		DebugContext context = new DebugContext();
//		context.setDebugEnabled(true);
//		IDebugListener listener = mock(IDebugListener.class);
//		context.setDebugListener(listener);
//
//		IDeploymentConfig dc = mockDeploymentConfig();
//		IClient client = mock(IClient.class);
//		when(dc.accept(any(), any())).thenReturn(client);
//
//		new MockRedeploymentJob(dc);
//		context.disableDebugging(dc, context, monitor);
//		verify(dc).setEnvironmentVariable("DEBUG", "false");
//
//		verify(listener).onPodRestart(isA(DebugContext.class), eq(monitor));
//
//		verify(client).update(dc);
	}

//	private IEnvironmentVariable createVar(String key, String value) {
//		IEnvironmentVariable var = mock(IEnvironmentVariable.class);
//		when(var.getName()).thenReturn(key);
//		when(var.getValue()).thenReturn(value);
//		return var;
//	}

//	private IDeploymentConfig mockDeploymentConfig(IEnvironmentVariable... vars) {
//		IDeploymentConfig dc = mock(IDeploymentConfig.class);
//		when(dc.getName()).thenReturn("foo-dc");
//		//when(dc.equals(dc)).thenReturn(Boolean.TRUE);
//		com.openshift.restclient.model.IProject project = mock(com.openshift.restclient.model.IProject.class);
//		IPod somePod = mock(IPod.class);
//		when(somePod.getAnnotation("openshift.io/deployment-config.name")).thenReturn("foo-dc");
//		when(project.getResources(ResourceKind.POD)).thenReturn(Arrays.asList(somePod));
//		when(dc.getProject()).thenReturn(project);
//		when(dc.getReplicaSelector()).thenReturn(Collections.singletonMap("foo", "bar"));
//		if (vars != null) {
//			when(dc.getEnvironmentVariables()).thenReturn(Arrays.asList(vars));
//		}
//		return dc;
//	}

//	private IServer mockServer(String name) {
//		IServer server = mock(IServer.class);
//		when(server.getName()).thenReturn(name);
//		return server;
//	}

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

}
