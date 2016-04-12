/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior;

import java.io.File;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7FSModuleStateVerifier;
import org.jboss.ide.eclipse.as.wtp.core.console.ServerConsoleModel;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public class OpenShiftEapModulesController extends JBoss7FSModuleStateVerifier implements ISubsystemController {

	public OpenShiftEapModulesController() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int getRootModuleState(IServer server, IModule root,
			String deploymentName, IProgressMonitor monitor) throws Exception {
		// do rsync, remote to local, then...
		syncDown(monitor);
		return super.getRootModuleState(server, root, deploymentName, monitor);
	}
	
	@Override
	public int changeModuleStateTo(IModule[] module, int state, IProgressMonitor monitor) throws CoreException {
		syncDown(monitor);
		super.changeModuleStateTo(module, state, monitor);
		syncUp(monitor);
		deleteMarkers(".dodeploy");
		return state;
	}
	
	private void deleteMarkers(String suffix) throws CoreException {
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		Stream.of(localDeploymentDirectory.listFiles())
		.filter(p->p.getName().endsWith(suffix))
		.forEach(p->p.delete());
	}
	
	private MultiStatus syncDown(IProgressMonitor monitor) throws CoreException {
		final RSync rsync = OpenShiftServerUtils.createRSync(getServer());
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
				NLS.bind("Could not sync all pods to folder {0}", localDeploymentDirectory.getAbsolutePath()), null);
		rsync.syncPodsToDirectory(localDeploymentDirectory, status, ServerConsoleModel.getDefault().getConsoleWriter());
		return status;
	}
	private MultiStatus syncUp(IProgressMonitor monitor) throws CoreException {
		// do rsync local to remote
		final RSync rsync = OpenShiftServerUtils.createRSync(getServer());
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		final MultiStatus status = new MultiStatus(OpenShiftCoreActivator.PLUGIN_ID, 0, 
				NLS.bind("Could not sync all pods to folder {0}", localDeploymentDirectory.getAbsolutePath()), null);
		rsync.syncDirectoryToPods(localDeploymentDirectory, status, ServerConsoleModel.getDefault().getConsoleWriter());
		return status;
	}
	
}
