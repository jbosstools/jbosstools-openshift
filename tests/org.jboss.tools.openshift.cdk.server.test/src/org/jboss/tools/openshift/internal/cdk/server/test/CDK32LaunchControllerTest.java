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
package org.jboss.tools.openshift.internal.cdk.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.openshift.cdk.server.test.CDKTestActivator;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK32Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK3Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDK3LaunchController;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class CDK32LaunchControllerTest {
	private static final String NAME = "cdk-test-123";
	
	
	private class CDK3TestLaunchController extends CDK3LaunchController {
		private IServer s23;
		private CDKServer cdk;

		public CDK3TestLaunchController(IServer s, CDKServer cdk) {
			s23 = s;
			this.cdk = cdk;
		}

		public void initialize(ILaunchConfigurationWorkingCopy wc) throws CoreException {
			initialize(wc, cdk.getUsername(), s23);
		}

		protected void performOverrides(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
			performOverrides(workingCopy, s23, cdk);
		}
	}

	@Test
	public void testInitializeAndOverrides() throws Exception {

		ILaunchConfigurationWorkingCopy wc = mock(ILaunchConfigurationWorkingCopy.class);
		when(wc.getAttribute(any(String.class), any(String.class))).thenAnswer(AdditionalAnswers.returnsSecondArg());
		String userName = "Drumpf";
		IServer server = mockServer();
		when(wc.getName()).thenReturn(NAME);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valCaptor = ArgumentCaptor.forClass(String.class);

		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		CDK3TestLaunchController controller = new CDK3TestLaunchController(server, cdkServer);
		controller.initialize(wc, userName, server);
		verify(wc, Mockito.times(1)).setAttribute(keyCaptor.capture(), valCaptor.capture());
		List<String> allVals = valCaptor.getAllValues();

		assertTrue(allVals.get(0).replace("\\", "/").endsWith(".metadata/.plugins/org.jboss.ide.eclipse.as.core"));
		
		
		ArgumentCaptor<String> keyCaptor2 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valCaptor2 = ArgumentCaptor.forClass(String.class);
		controller.performOverrides(wc);
		verify(wc, Mockito.times(4)).setAttribute(keyCaptor2.capture(), valCaptor2.capture());
		allVals = valCaptor2.getAllValues();
		assertTrue(allVals.get(0).replace("\\", "/").endsWith(".metadata/.plugins/org.jboss.ide.eclipse.as.core"));
		assertTrue(allVals.get(1).replace("\\", "/").endsWith(".metadata/.plugins/org.jboss.ide.eclipse.as.core"));
		assertTrue(allVals.get(2).endsWith("/home/user/apps/minishift"));
		assertTrue(allVals.get(3).endsWith("--profile minishift start --vm-driver=virtualbox"));
	}

	@Test
	public void testSetupWithMinishiftHome() throws Exception {

		ILaunchConfigurationWorkingCopy wc = mock(ILaunchConfigurationWorkingCopy.class);
		when(wc.getAttribute(any(String.class), any(String.class))).thenAnswer(AdditionalAnswers.returnsSecondArg());
		when(wc.getName()).thenReturn(NAME);

		String userName = "Drumpf";
		IPath msHome = CDKTestActivator.getDefault().getStateLocation().append("test927");
		msHome.toFile().mkdirs();
		IServer server = mockServerWithMSHome(msHome);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		Class<Map<String, String>> mapClass = (Class<Map<String, String>>) (Class) Map.class;
		ArgumentCaptor<Map<String, String>> valCaptor = ArgumentCaptor.forClass(mapClass);

		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		CDK3TestLaunchController controller = new CDK3TestLaunchController(server, cdkServer);
		controller.setupLaunchConfiguration(wc, new NullProgressMonitor());
		verify(wc, Mockito.times(2)).setAttribute(keyCaptor.capture(), valCaptor.capture());
		List<String> allKeys = keyCaptor.getAllValues();
		List<Map<String, String>> allVals = valCaptor.getAllValues();

		Map<String, String> latest = allVals.get(allVals.size() - 1);
		String val1 = latest.get("MINISHIFT_HOME");
		assertEquals(msHome.toOSString(), val1);
	}

	private IServer mockServerWithMSHome(IPath home2) {
		IServer server = mockServer();
		String home = System.getProperty("user.home");
		String defaultMinishiftHome = new File(home, CDKConstants.CDK_RESOURCE_DOTMINISHIFT).getAbsolutePath();
		when(server.getAttribute(CDK32Server.MINISHIFT_HOME, defaultMinishiftHome)).thenReturn(home2.toOSString());
		return server;
	}

	private IServer mockServer() {
		IServer server = mock(IServer.class);
		when(server.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false)).thenReturn(Boolean.TRUE);
		when(server.getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME))
				.thenReturn(CDKConstants.CDK_ENV_SUB_USERNAME);
		when(server.getAttribute(CDK3Server.PROP_HYPERVISOR, CDK3Server.getHypervisors()[0]))
				.thenReturn(CDK3Server.VIRTUALBOX);
		when(server.getAttribute(CDK32Server.PROFILE_ID, (String) null))
				.thenReturn(CDK32Server.MINISHIFT_DEFAULT_PROFILE);
		when(server.getAttribute(CDK3Server.MINISHIFT_FILE, (String) null)).thenReturn("/home/user/apps/minishift");
		when(server.getName()).thenReturn(NAME);

		CDKServer cdk = mock(CDKServer.class);
		when(server.loadAdapter(eq(CDKServer.class), any(IProgressMonitor.class))).thenReturn(cdk);
		when(cdk.passCredentials()).thenReturn(Boolean.TRUE);
		when(cdk.getUserEnvironmentKey()).thenReturn(CDKConstants.CDK_ENV_SUB_USERNAME);
		IServerType mockType = mock(IServerType.class);
		when(server.getServerType()).thenReturn(mockType);
		when(mockType.getId()).thenReturn(CDK32Server.CDK_V32_SERVER_TYPE);
		return server;
	}
}
