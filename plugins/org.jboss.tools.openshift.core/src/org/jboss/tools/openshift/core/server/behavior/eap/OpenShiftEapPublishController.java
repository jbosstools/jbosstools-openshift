/*******************************************************************************
 * Copyright (c) 2017-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.eap;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.v7.DeploymentMarkerUtils;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.common.util.FileUtils;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

public class OpenShiftEapPublishController extends OpenShiftPublishController implements ISubsystemController {

	private static final long WAIT_FOR_UNDEPLOYED_TIMEOUT = 20l * 1000l; // 10s
	private static final long UNDEPLOYED_CHECKS = 10;

	@Override
	public int publishModule(int kind, int deltaKind, IModule[] module, IProgressMonitor monitor) throws CoreException {
		return super.publishModule(IServer.PUBLISH_CLEAN, deltaKind, module, monitor);
	}

	@Override
	protected File deleteDeploymentFile(File moduleDeployment, IProgressMonitor monitor) throws CoreException {
		try {
			File moduleDeploymentFile = super.deleteDeploymentFile(moduleDeployment, monitor);
			if (moduleDeploymentFile != null) {
				triggerUndeploy(moduleDeploymentFile);
				// allow server to undeploy first by syncing right after remove
				syncDirectoryToPods(monitor);
				// wait for undeployed marker to show up
				waitForUndeployed(moduleDeployment, monitor);
			}
			return moduleDeploymentFile;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private void triggerUndeploy(File moduleDeployment) {
		deleteFiles(new File(moduleDeployment + DeploymentMarkerUtils.DEPLOYED),
				new File(moduleDeployment + DeploymentMarkerUtils.DO_DEPLOY),
				new File(moduleDeployment + DeploymentMarkerUtils.DEPLOYING));
	}

	private void deleteFiles(File... files) {
		Arrays.stream(files).forEach(file -> {
			if (file.exists()) {
				FileUtils.remove(file);
			}
		});
	}

	private void waitForUndeployed(File moduleDeployment, IProgressMonitor monitor) throws CoreException, InterruptedException {
		long start = System.currentTimeMillis();
		do {
				Thread.sleep(WAIT_FOR_UNDEPLOYED_TIMEOUT/UNDEPLOYED_CHECKS);
				syncPodsToDirectory(monitor);
		}
		while(!isUndeployed(moduleDeployment)
				&& System.currentTimeMillis() < start + WAIT_FOR_UNDEPLOYED_TIMEOUT
				&& !monitor.isCanceled());
	}

	private boolean isUndeployed(File moduleDeployment) {
		return fileExists(new File(moduleDeployment + DeploymentMarkerUtils.UNDEPLOYED),
				new File(moduleDeployment + DeploymentMarkerUtils.FAILED_DEPLOY));
	}

	private boolean fileExists(File... files) {
		return Arrays.stream(files)
			.anyMatch(File::exists);
	}
}
