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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IFilesystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.as.core.server.controllable.systems.IDeploymentOptionsController;
import org.jboss.tools.as.core.server.controllable.systems.IModuleDeployPathController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.eap.OpenshiftEapProfileDetector;
import org.jboss.tools.openshift.test.core.server.util.OpenShiftServerTestUtils;
import org.junit.After;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * This test is to verify that the correct subsystems are used for each 
 * of the various systems. 
 * 
 * @author rob
 *
 */
public class OpenShiftSubsystemTest extends TestCase {

	@Override
	@After
	public void tearDown() {
		OpenShiftServerTestUtils.cleanup();
	}
	
	@Test 
	public void testSubsystemsNull() throws Exception {
		IServer s1 =  OpenShiftServerTestUtils.createOpenshift3Server("example", null);
		testOpenshift3Standard(s1);
	}

	@Test 
	public void testSubsystemsStandard() throws Exception {
		IServer s1 =  OpenShiftServerTestUtils.createOpenshift3Server("example", OpenShiftServerBehaviour.PROFILE_OPENSHIFT3);
		testOpenshift3Standard(s1);
	}

	private void testOpenshift3Standard(IServer s1) throws CoreException {
		IControllableServerBehavior beh = (IControllableServerBehavior)s1.loadAdapter(IControllableServerBehavior.class, new NullProgressMonitor());
		String[] systems = new String[]{
				IControllableServerBehavior.SYSTEM_LAUNCH, IControllableServerBehavior.SYSTEM_MODULES,
				IControllableServerBehavior.SYSTEM_PUBLISH, IControllableServerBehavior.SYSTEM_SHUTDOWN,
				IFilesystemController.SYSTEM_ID, IDeploymentOptionsController.SYSTEM_ID, 
				IModuleDeployPathController.SYSTEM_ID
		};
		String[] expected = new String[]{
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController",
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpModuleController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftShutdownController",
			"org.jboss.ide.eclipse.as.wtp.core.server.behavior.LocalFilesystemController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftDeploymentOptionsController",
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.ModuleDeployPathController",
		};
		for( int i = 0; i < systems.length; i++ ) {
			ISubsystemController c = beh.getController(systems[i]);
			assertEquals(expected[i], c.getClass().getName());
		}
	}
	
	@Test
	public void testSubsystemsEAP() throws Exception {
		IServer s1 = OpenShiftServerTestUtils.createOpenshift3Server("example", OpenshiftEapProfileDetector.PROFILE);
		IControllableServerBehavior beh = (IControllableServerBehavior)s1.loadAdapter(IControllableServerBehavior.class, new NullProgressMonitor());
		String[] systems = new String[]{
				IControllableServerBehavior.SYSTEM_LAUNCH, IControllableServerBehavior.SYSTEM_MODULES,
				IControllableServerBehavior.SYSTEM_PUBLISH, IControllableServerBehavior.SYSTEM_SHUTDOWN,
				IFilesystemController.SYSTEM_ID, IDeploymentOptionsController.SYSTEM_ID, 
				IModuleDeployPathController.SYSTEM_ID
		};
		String[] expected = new String[]{
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController",
			"org.jboss.tools.openshift.core.server.behavior.eap.OpenShiftEapModulesController",
			"org.jboss.tools.openshift.core.server.behavior.eap.OpenShiftEapPublishController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftShutdownController",
			"org.jboss.ide.eclipse.as.wtp.core.server.behavior.LocalFilesystemController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftDeploymentOptionsController",
			"org.jboss.tools.openshift.core.server.behavior.eap.OpenShiftEapDeployPathController",
		};
		for( int i = 0; i < systems.length; i++ ) {
			ISubsystemController c = beh.getController(systems[i]);
			assertEquals(expected[i], c.getClass().getName());
		}
	}
}
