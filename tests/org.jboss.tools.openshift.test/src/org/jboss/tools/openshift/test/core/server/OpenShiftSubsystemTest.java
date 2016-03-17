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
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IFilesystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.as.core.server.controllable.systems.IDeploymentOptionsController;
import org.jboss.tools.as.core.server.controllable.systems.IModuleDeployPathController;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
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

	@After
	public void tearDown() {
		OpenShiftServerTestUtility.cleanup();
	}
	
	@Test 
	public void testSubsystemsNull() throws Exception {
		IServer s1 = createOpenshift3Server("example", null);
		testOpenshift3Standard(s1);
	}

	@Test 
	public void testSubsystemsStandard() throws Exception {
		IServer s1 = createOpenshift3Server("example", OpenShiftServerBehaviour.PROFILE_OPENSHIFT3);
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
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpLaunchController",
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpModuleController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController",
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpShutdownController",
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
		IServer s1 = createOpenshift3Server("example", OpenShiftServerBehaviour.PROFILE_OPENSHIFT3_EAP);
		IControllableServerBehavior beh = (IControllableServerBehavior)s1.loadAdapter(IControllableServerBehavior.class, new NullProgressMonitor());
		String[] systems = new String[]{
				IControllableServerBehavior.SYSTEM_LAUNCH, IControllableServerBehavior.SYSTEM_MODULES,
				IControllableServerBehavior.SYSTEM_PUBLISH, IControllableServerBehavior.SYSTEM_SHUTDOWN,
				IFilesystemController.SYSTEM_ID, IDeploymentOptionsController.SYSTEM_ID, 
				IModuleDeployPathController.SYSTEM_ID
		};
		String[] expected = new String[]{
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpLaunchController",
			"org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7FSModuleStateVerifier",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController",
			"org.jboss.tools.as.core.server.controllable.subsystems.internal.NoOpShutdownController",
			"org.jboss.ide.eclipse.as.wtp.core.server.behavior.LocalFilesystemController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftDeploymentOptionsController",
			"org.jboss.tools.openshift.core.server.behavior.OpenShiftEapDeployPathController",
		};
		for( int i = 0; i < systems.length; i++ ) {
			ISubsystemController c = beh.getController(systems[i]);
			assertEquals(expected[i], c.getClass().getName());
		}
	}
	
	
	private IServer createOpenshift3Server(String name, String profile) throws CoreException {
		IServerType type = ServerCore.findServerType("org.jboss.tools.openshift.server.type");
		IServerWorkingCopy wc = type.createServer(name, null, null);
		OpenShiftServerUtils.updateServer(name, "http://www.example.com", "dummy", 
				"dummy", "dummy", "dummy", "dummy", "dummy", wc);
		if( profile != null ) {
			ServerProfileModel.setProfile(wc, profile);
		}
		return wc.save(false, null);
	}
}
