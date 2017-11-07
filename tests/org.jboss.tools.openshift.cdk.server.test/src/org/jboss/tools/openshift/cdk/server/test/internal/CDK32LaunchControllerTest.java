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
package org.jboss.tools.openshift.cdk.server.test.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDK3LaunchController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class CDK32LaunchControllerTest {
	
	private CDK3LaunchController controller;
	
	@Before
	public void setUp() {
		controller = new CDK3LaunchController();
	}

	@Test
	public void testInitialize() throws Exception {
		
		ILaunchConfigurationWorkingCopy wc = mock(ILaunchConfigurationWorkingCopy.class);
		when(wc.getAttribute(any(String.class), any(String.class))).thenAnswer(AdditionalAnswers.returnsSecondArg());
		String userName = "Drumpf";
		IServer server = mockServer();
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valCaptor = ArgumentCaptor.forClass(String.class);
		
		
		controller.initialize(wc, userName, server);
		verify(wc, Mockito.times(3)).setAttribute(keyCaptor.capture(), valCaptor.capture());
		List<String> allKeys = keyCaptor.getAllValues();
		List<String> allVals = valCaptor.getAllValues();
		
		assertTrue(allVals.get(0).replace("\\", "/").endsWith(".metadata/.plugins/org.jboss.ide.eclipse.as.core"));
		assertTrue(allVals.get(1).endsWith("/home/user/apps/minishift"));
		assertTrue(allVals.get(2).endsWith("--profile minishift start --vm-driver=virtualbox"));
	}
	
	private IServer mockServer() {
		IServer server = mock(IServer.class);
		when(server.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false)).thenReturn(Boolean.TRUE);
		when(server.getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME)).thenReturn(CDKConstants.CDK_ENV_SUB_USERNAME);
		when(server.getAttribute(CDK3Server.PROP_HYPERVISOR, CDK3Server.getHypervisors()[0])).thenReturn(CDK3Server.VIRTUALBOX);
		when(server.getAttribute(CDK32Server.PROFILE_ID, (String)null)).thenReturn(CDK32Server.MINISHIFT_DEFAULT_PROFILE);
		when(server.getAttribute(CDK3Server.MINISHIFT_FILE, (String) null)).thenReturn("/home/user/apps/minishift");
		
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
