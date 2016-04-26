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

import static org.mockito.Mockito.when;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftEapDeployPathController.OpenShiftModuleDeploymentPrefsUtil;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class OpenShiftModuleDeploymentPrefsUtilTest extends TestCase {
	@Mock
	private IModule module1;
	@Mock
	private IModule module2;

	@Test
	public void testOutputName() throws Exception {
		// Create server
		IServer s1 =  OpenShiftServerTestUtils.createOpenshift3Server("example", OpenShiftServerBehaviour.PROFILE_OPENSHIFT3);
		
		// Make a web module 
		when(module1.getName()).thenReturn("webProject");
		when(module1.getModuleType()).thenReturn(getWebModuleType());
		// Make a utility that will simulates finding the web module from the server
		OpenShiftModuleDeploymentPrefsUtil2 module1Util = getUtilForModule(module1);
		String outputNameMatched = module1Util.getOutputNameFromSettings2(s1, module1);
		assertEquals("ROOT.war", outputNameMatched);
		
		// Make an ejb module
		when(module2.getName()).thenReturn("ejbProject");
		when(module2.getModuleType()).thenReturn(getEjbModuleType());
		// Make a utility that will simulates finding the ejb module from the server
		OpenShiftModuleDeploymentPrefsUtil2 module2Util = getUtilForModule(module2);
		String outputNameUnmatched = module2Util.getOutputNameFromSettings2(s1, module1);
		assertNull(outputNameUnmatched);
	}

	private IModuleType getWebModuleType() {
		return getModuleType("2.5", "jst.web");
	}

	private IModuleType getEjbModuleType() {
		return getModuleType("3.0", "jst.ejb");
	}
	

	private IModuleType getModuleType(final String version, final String id) {
		return new IModuleType() {
			@Override
			public String getVersion() {
				return version;
			}
			
			@Override
			public String getName() {
				return id;
			}
			
			@Override
			public String getId() {
				return id;
			}
		};
	}
	
	// Create a utility that 'finds' the module for the project listed on the server
	private OpenShiftModuleDeploymentPrefsUtil2 getUtilForModule(final IModule m) {
		OpenShiftModuleDeploymentPrefsUtil2 util = new OpenShiftModuleDeploymentPrefsUtil2() {
			@Override
			protected IModule findProjectModule(IServerAttributes server) {
				return m;
			}
		};
		return util;
	}
	
	// Just expose the getOutputNameFromSettings method 
	private static class OpenShiftModuleDeploymentPrefsUtil2 extends OpenShiftModuleDeploymentPrefsUtil {
		public String getOutputNameFromSettings2(IServerAttributes server, IModule module) {
			return super.getOutputNameFromSettings(server, module);
		}
	}

	@Override
	@After
	public void tearDown() {
		OpenShiftServerTestUtils.cleanup();
	}
}
