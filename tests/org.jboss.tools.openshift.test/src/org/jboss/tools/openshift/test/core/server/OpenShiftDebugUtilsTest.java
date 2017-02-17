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

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.core.server.debug.DebuggingContext;
import org.jboss.tools.openshift.internal.core.server.debug.ReplicationControllerListenerJob;
import org.jboss.tools.openshift.internal.core.server.debug.IDebugListener;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IPod;

public class OpenShiftDebugUtilsTest {

	
	private IProgressMonitor monitor;
	private OpenShiftDebugUtils debugUtils;
	private ILaunchManager launchManager;
	
	@Before
	public void setUp() {
		monitor = new NullProgressMonitor();
		launchManager = mock(ILaunchManager.class);
		debugUtils = OpenShiftDebugUtils.get(launchManager);
		System.setProperty(ReplicationControllerListenerJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, "2000");
	}
	
	@After
	public void tearDown() {
		System.clearProperty(ReplicationControllerListenerJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY);
	}
	

	@Test
	public void testGetDebuggingContext() {
		assertNull(debugUtils.getDebuggingContext(null));
		
		IDeploymentConfig dc = mockDeploymentConfig();
		DebuggingContext context = debugUtils.getDebuggingContext(dc);
		assertEquals(-1, context.getDebugPort());
		assertFalse(context.isDebugEnabled());
		
		List<IEnvironmentVariable> vars = Arrays.asList(createVar("DEBUG_PORT", "9797"), createVar("DEBUG", "true"));
		when(dc.getEnvironmentVariables()).thenReturn(vars);
		context = debugUtils.getDebuggingContext(dc);
		
		assertEquals(9797, context.getDebugPort());
		assertTrue(context.isDebugEnabled());
	
		vars = Arrays.asList(createVar("DEBUG_PORT", "1234"), createVar("DEV_MODE", "true"));
		when(dc.getEnvironmentVariables()).thenReturn(vars);
		context = debugUtils.getDebuggingContext(dc);
		
		assertEquals(1234, context.getDebugPort());
		// JBIDE-23961
		// assertTrue(context.isDebugEnabled());
		assertFalse(context.isDebugEnabled());
		
	}
	
	
	@Test
	public void testEnableDebug() throws CoreException {
		DebuggingContext context = new DebuggingContext();
		context.setDebugPort(1234);
		IDebugListener listener = mock(IDebugListener.class);
		context.setDebugListener(listener);
		

		IDeploymentConfig dc = mockDeploymentConfig();
		IClient client = mock(IClient.class);
		when(dc.accept(any(), any())).thenReturn(client);
		new MockRedeploymentJob(dc);
		debugUtils.enableDebugMode(dc, context, monitor);
		
		verify(dc).setEnvironmentVariable("DEBUG_PORT", "1234");
		verify(dc).setEnvironmentVariable("DEV_MODE", "true");
		verify(dc).setEnvironmentVariable("DEBUG", "true");
		verify(listener).onPodRestart(isA(DebuggingContext.class), eq(monitor));
		verify(client).update(dc);
		IPod pod = context.getPod();
		assertNotNull(pod);
		assertNotNull("RunningPod", pod.getName());
	}

	
	@Test
	public void testAlreadyDebugging() throws CoreException {
		DebuggingContext context = new DebuggingContext();
		context.setDebugPort(1234);
		context.setDebugEnabled(true);
		IDebugListener listener = mock(IDebugListener.class);
		context.setDebugListener(listener);
		
		IDeploymentConfig dc = mockDeploymentConfig();
		
		debugUtils.enableDebugMode(dc, context, monitor);
		
		verify(listener).onDebugChange(isA(DebuggingContext.class), eq(monitor));
	}
	
	@Test
	public void testDisableDebug() throws CoreException {
		DebuggingContext context = new DebuggingContext();
		context.setDebugEnabled(true);
		IDebugListener listener = mock(IDebugListener.class);
		context.setDebugListener(listener);
		

		IDeploymentConfig dc = mockDeploymentConfig();
		IClient client = mock(IClient.class);
		when(dc.accept(any(), any())).thenReturn(client);
		
		new MockRedeploymentJob(dc);
		debugUtils.disableDebugMode(dc, context, monitor);
		
		verify(dc).setEnvironmentVariable("DEV_MODE", "false");
		verify(dc).setEnvironmentVariable("DEBUG", "false");
		
		verify(listener).onPodRestart(isA(DebuggingContext.class), eq(monitor));

		verify(client).update(dc);
	}
	
	@Test
	public void testSetupRemoteDebuggerLaunchConfiguration() throws CoreException {
		ILaunchConfigurationWorkingCopy workingCopy = mock(ILaunchConfigurationWorkingCopy.class);
		
		IProject project = mock(IProject.class);
		String name = "baymax";
		when(project.getName()).thenReturn(name);
		
		debugUtils.setupRemoteDebuggerLaunchConfiguration(workingCopy, project, 1234);
		
		//pretty stoopid test
		verify(workingCopy).setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,name);
		Map<String, String> connectMap = new HashMap<>();
		connectMap.put("port", "1234"); //$NON-NLS-1$
		connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
		verify(workingCopy).setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);
	}
	
	@Test
	public void getRemoteDebuggerLaunchConfiguration() throws CoreException {
		String name = "Remote debugger to foo";
		ILaunchConfiguration good = mock(ILaunchConfiguration.class);
		when(good.getName()).thenReturn(name);
		ILaunchConfiguration bad = mock(ILaunchConfiguration.class);
		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[]{bad, good});
		
		IServer server = mockServer("foo");
		assertSame(good, debugUtils.getRemoteDebuggerLaunchConfiguration(server));
		
		server = mockServer("bar");
		assertNull(debugUtils.getRemoteDebuggerLaunchConfiguration(server));
	}
	
	@Test
	public void testCreateRemoteDebuggerLaunchConfiguration() throws CoreException {
		IServer server = mockServer("foo");
		ILaunchConfigurationType launchConfigurationType = mock(ILaunchConfigurationType.class);
		when(launchManager.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION)).thenReturn(launchConfigurationType);
		
		debugUtils.createRemoteDebuggerLaunchConfiguration(server);
		
		verify(launchConfigurationType).newInstance(null, "Remote debugger to foo");
	}
	
	
	@Test
	public void testTerminateRemoteDebugger() throws CoreException {
		ILaunchConfiguration launchConfig = mock(ILaunchConfiguration.class);
		
		String name = "foo";
		IServer server = mockServer(name);
		when(launchConfig.getName()).thenReturn("Remote debugger to "+ name);
		
		ILaunch matchingLaunch1 = mock(ILaunch.class);
		when(matchingLaunch1.getLaunchConfiguration()).thenReturn(launchConfig);

		ILaunch matchingLaunch2 = mock(ILaunch.class);
		when(matchingLaunch2.getLaunchConfiguration()).thenReturn(launchConfig);
		when(matchingLaunch2.canTerminate()).thenReturn(true);
		
		ILaunch otherLaunch1 = mock(ILaunch.class);
		ILaunch otherLaunch2 = mock(ILaunch.class);
		
		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[]{launchConfig});
		
		when(launchManager.getLaunches()).thenReturn(new ILaunch[]{otherLaunch1, matchingLaunch1, matchingLaunch2, otherLaunch2});
		
		debugUtils.terminateRemoteDebugger(server);
		
		verify(matchingLaunch1, never()).terminate();
		verify(matchingLaunch2).terminate();
		verify(otherLaunch1, never()).terminate();
		verify(otherLaunch2, never()).terminate();
	}
	
	@Test
	public void testTerminateRemoteDebuggerWithException() throws CoreException {
		String name = "foo";
		IServer server = mockServer(name);
		
		ILaunchConfiguration launchConfig = mock(ILaunchConfiguration.class);
		when(launchConfig.getName()).thenReturn("Remote debugger to "+ name);
		
		ILaunch matchingLaunch1 = mock(ILaunch.class);
		when(matchingLaunch1.getLaunchConfiguration()).thenReturn(launchConfig);
		when(matchingLaunch1.canTerminate()).thenReturn(true);
		IStatus error = new Status(IStatus.ERROR, "foo", "buuuurned!", null);
		doThrow(new DebugException(error)).when(matchingLaunch1).terminate();
		
		ILaunch matchingLaunch2 = mock(ILaunch.class);
		when(matchingLaunch2.canTerminate()).thenReturn(true);
		when(matchingLaunch2.getLaunchConfiguration()).thenReturn(launchConfig);
		
		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[]{launchConfig});
		
		when(launchManager.getLaunches()).thenReturn(new ILaunch[]{matchingLaunch1, matchingLaunch2});
		
		
		try {
			debugUtils.terminateRemoteDebugger(server);
			fail();
		} catch (CoreException e) {
			verify(matchingLaunch2).terminate();
			assertEquals(error, e.getStatus().getChildren()[0]);
		}
	}
	
	private IEnvironmentVariable createVar(String key, String value) {
		IEnvironmentVariable var = mock(IEnvironmentVariable.class);
		when(var.getName()).thenReturn(key);
		when(var.getValue()).thenReturn(value);
		return var;
	}
	
	private IDeploymentConfig mockDeploymentConfig() {
		IDeploymentConfig dc = mock(IDeploymentConfig.class);
		when(dc.getName()).thenReturn("foo-dc");
		//when(dc.equals(dc)).thenReturn(Boolean.TRUE);
		com.openshift.restclient.model.IProject project = mock(com.openshift.restclient.model.IProject.class);
		IPod somePod = mock(IPod.class);
		when(somePod.getAnnotation("openshift.io/deployment-config.name")).thenReturn("foo-dc");
		when(project.getResources(ResourceKind.POD)).thenReturn(Arrays.asList(somePod));
		when(dc.getProject()).thenReturn(project);
		when(dc.getReplicaSelector()).thenReturn(Collections.singletonMap("foo", "bar"));
		return dc;
	}
	
	private IServer mockServer(String name) {
		IServer server = mock(IServer.class);
		when(server.getName()).thenReturn(name);
		return server;
	}
	
	private static class MockRedeploymentJob extends Job {

		private IDeploymentConfig dc;

		public MockRedeploymentJob(IDeploymentConfig dc) {
			super("MockRedeploymentJob");
			this.dc = dc;
			schedule(600);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IConnection connection = mock(IConnection.class);
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, mock(IDeploymentConfig.class), mock(IDeploymentConfig.class));
			waitFor(50);
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, dc, dc);
			waitFor(50);
			IPod deployPod = mock(IPod.class);
			when(deployPod.getName()).thenReturn("foo-deploy");
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, null, deployPod);
			waitFor(50);
			IPod pendingPod = mock(IPod.class);
			when(pendingPod.getStatus()).thenReturn("Pending");
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, deployPod, pendingPod);
			waitFor(100);
			IPod runningPod = mock(IPod.class);
			when(runningPod.getStatus()).thenReturn("Running");
			when(runningPod.getName()).thenReturn("RunningPod");
			when(runningPod.getLabels()).thenReturn(Collections.singletonMap("foo", "bar"));
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, null, pendingPod, runningPod);
			return Status.OK_STATUS;
		}
		
		private void waitFor(long millis) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
