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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKConstants;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDKServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CDKLaunchController;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants;
import org.junit.Before;
import org.junit.Test;

public class CDKLaunchControllerTest {

	private CDKLaunchController controller;

	@Before
	public void setUp() {
		controller = new CDKLaunchController();
	}

	@Test
	public void testInitialize() throws Exception {
		ILaunchConfigurationWorkingCopy wc = mock(ILaunchConfigurationWorkingCopy.class);
		String userName = "Drumpf";
		IServer server = mockServer();

		controller.initialize(wc, userName, server);

		Map<String, String> env = Collections.singletonMap(CDKConstants.CDK_ENV_SUB_USERNAME, userName);
		verify(wc).setAttribute(CDKLaunchController.FLAG_INITIALIZED, true);
		verify(wc).setAttribute(eq(IExternalLaunchConstants.ENVIRONMENT_VARS_KEY), eq(env));
		verify(wc).setAttribute(IExternalLaunchConstants.ATTR_ARGS, "up --no-color");
		verify(wc).setAttribute(eq(IExternalLaunchConstants.ATTR_LOCATION), (String)any());
	}

	private IServer mockServer() {
		IServer server = mock(IServer.class);
		when(server.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false)).thenReturn(Boolean.TRUE);
		when(server.getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK_ENV_SUB_USERNAME))
				.thenReturn(CDKConstants.CDK_ENV_SUB_USERNAME);

		CDKServer cdk = mock(CDKServer.class);

		when(cdk.passCredentials()).thenReturn(Boolean.TRUE);
		when(cdk.getUserEnvironmentKey()).thenReturn(CDKConstants.CDK_ENV_SUB_USERNAME);
		when(server.loadAdapter(eq(CDKServer.class), any(IProgressMonitor.class))).thenReturn(cdk);
		return server;
	}
}
