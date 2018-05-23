/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.eap;

import java.io.File;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.v7.DeploymentMarkerUtils;
import org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7FSModuleStateVerifier;
import org.jboss.ide.eclipse.as.wtp.core.console.ServerConsoleModel;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.server.RSync;

public class OpenShiftEapModulesController extends JBoss7FSModuleStateVerifier implements ISubsystemController {

	@Override
	protected int getRootModuleState(IServer server, IModule root, String deploymentName, IProgressMonitor monitor)
			throws Exception {
		syncDown(monitor);
		return super.getRootModuleState(server, root, deploymentName, monitor);
	}

	@Override
	public int changeModuleStateTo(IModule[] module, int state, IProgressMonitor monitor) throws CoreException {
		syncDown(monitor);
		super.changeModuleStateTo(module, state, monitor);
		syncUp(monitor);
		deleteMarkers(DeploymentMarkerUtils.DO_DEPLOY);
		return state;
	}

	private void deleteMarkers(String suffix) throws CoreException {
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		Stream.of(localDeploymentDirectory.listFiles())
			.filter(file -> file.getName().endsWith(suffix))
			.forEach(File::delete);
	}

	/**
	 * RSyncs the pods to the local folder.
	 * 
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	private MultiStatus syncDown(IProgressMonitor monitor) throws CoreException {
		final RSync rsync = OpenShiftServerUtils.createRSync(getServer(), monitor);
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		return rsync.syncPodsToDirectory(localDeploymentDirectory, ServerConsoleModel.getDefault().getConsoleWriter());
	}

	/**
	 * RSyncs the local folders to the pods.
	 * 
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	protected MultiStatus syncUp(IProgressMonitor monitor) throws CoreException {
		final RSync rsync = OpenShiftServerUtils.createRSync(getServer(), monitor);
		final File localDeploymentDirectory = new File(getDeploymentOptions().getDeploymentsRootFolder(true));
		return rsync.syncDirectoryToPods(localDeploymentDirectory, ServerConsoleModel.getDefault().getConsoleWriter());
	}
}
