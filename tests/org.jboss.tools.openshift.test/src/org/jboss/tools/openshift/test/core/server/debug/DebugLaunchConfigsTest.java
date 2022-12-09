/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.debug;

import static org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.core.server.debug.DebugLaunchConfigs;
import org.jboss.tools.openshift.internal.core.util.NewPodDetectorJob;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DebugLaunchConfigsTest {

	private ILaunchManager launchManager;
	private DebugLaunchConfigs debugLaunchConfigs;

	@Before
	public void setUp() {
		this.launchManager = mock(ILaunchManager.class);
		this.debugLaunchConfigs = DebugLaunchConfigs.get(launchManager);
		System.setProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY, "2000");
	}

	@After
	public void tearDown() {
		System.clearProperty(NewPodDetectorJob.DEPLOYMENT_CONFIG_LISTENER_JOB_TIMEOUT_KEY);
	}

	@Test
	public void testSetupRemoteDebuggerLaunchConfiguration() throws CoreException {
		ILaunchConfigurationWorkingCopy workingCopy = mock(ILaunchConfigurationWorkingCopy.class);

		IProject project = mock(IProject.class);
		String name = "baymax";
		when(project.getName()).thenReturn(name);

		debugLaunchConfigs.setupRemoteDebuggerLaunchConfiguration(workingCopy, project, 1234);

		//pretty stoopid test
		verify(workingCopy).setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
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
		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[] { bad, good });

		IServer server = mockServer("foo");
		assertSame(good, debugLaunchConfigs.getRemoteDebuggerLaunchConfiguration(server));

		server = mockServer("bar");
		assertNull(debugLaunchConfigs.getRemoteDebuggerLaunchConfiguration(server));
	}

	@Test
	public void testCreateRemoteDebuggerLaunchConfiguration() throws CoreException {
		IServer server = mockServer("foo");
		ILaunchConfigurationType launchConfigurationType = mock(ILaunchConfigurationType.class);
		when(launchManager.getLaunchConfigurationType(ID_REMOTE_JAVA_APPLICATION)).thenReturn(launchConfigurationType);

		debugLaunchConfigs.createRemoteDebuggerLaunchConfiguration(server);

		verify(launchConfigurationType).newInstance(null, "Remote debugger to foo");
	}

	@Test
	public void testTerminateRemoteDebugger() throws CoreException {
		ILaunchConfiguration launchConfig = mock(ILaunchConfiguration.class);

		String name = "foo";
		IServer server = mockServer(name);
		when(launchConfig.getName()).thenReturn("Remote debugger to " + name);

		ILaunch matchingLaunch1 = mock(ILaunch.class);
		when(matchingLaunch1.getLaunchConfiguration()).thenReturn(launchConfig);

		ILaunch matchingLaunch2 = mock(ILaunch.class);
		when(matchingLaunch2.getLaunchConfiguration()).thenReturn(launchConfig);
		when(matchingLaunch2.canTerminate()).thenReturn(true);

		ILaunch otherLaunch1 = mock(ILaunch.class);
		ILaunch otherLaunch2 = mock(ILaunch.class);

		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[] { launchConfig });

		when(launchManager.getLaunches())
				.thenReturn(new ILaunch[] { otherLaunch1, matchingLaunch1, matchingLaunch2, otherLaunch2 });

		debugLaunchConfigs.terminateRemoteDebugger(server);

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
		when(launchConfig.getName()).thenReturn("Remote debugger to " + name);

		ILaunch matchingLaunch1 = mock(ILaunch.class);
		when(matchingLaunch1.getLaunchConfiguration()).thenReturn(launchConfig);
		when(matchingLaunch1.canTerminate()).thenReturn(true);
		IStatus error = new Status(IStatus.ERROR, "foo", "buuuurned!", null);
		doThrow(new DebugException(error)).when(matchingLaunch1).terminate();

		ILaunch matchingLaunch2 = mock(ILaunch.class);
		when(matchingLaunch2.canTerminate()).thenReturn(true);
		when(matchingLaunch2.getLaunchConfiguration()).thenReturn(launchConfig);

		when(launchManager.getLaunchConfigurations(any())).thenReturn(new ILaunchConfiguration[] { launchConfig });

		when(launchManager.getLaunches()).thenReturn(new ILaunch[] { matchingLaunch1, matchingLaunch2 });

		try {
			debugLaunchConfigs.terminateRemoteDebugger(server);
			fail();
		} catch (CoreException e) {
			verify(matchingLaunch2).terminate();
			assertEquals(error, e.getStatus().getChildren()[0]);
		}
	}

	private IServer mockServer(String name) {
		IServer server = mock(IServer.class);
		when(server.getName()).thenReturn(name);
		return server;
	}
}
