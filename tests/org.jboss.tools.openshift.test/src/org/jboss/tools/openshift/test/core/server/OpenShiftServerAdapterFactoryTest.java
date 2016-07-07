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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.IServerModule;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.internal.ui.server.OpenShiftServerAdapterFactory;
import org.junit.Test;

public class OpenShiftServerAdapterFactoryTest {

	@Test
	public void testLoadWebModule() {
		OpenShiftServerAdapterFactory factory = new OpenShiftServerAdapterFactory();
		IServer server = mock(IServer.class);
		OpenShiftServer realServer = mock(OpenShiftServer.class);
		when(server.loadAdapter(eq(OpenShiftServer.class), isA(IProgressMonitor.class))).thenReturn(realServer);
		IServerModule serverModule = factory.getAdapter(server, IServerModule.class);
		assertNotNull(serverModule);
		
		IModule[] module = serverModule.getModule();
		assertNotNull(module);
		
		assertNotNull(module[0].loadAdapter(IWebModule.class, null));
	}
}
