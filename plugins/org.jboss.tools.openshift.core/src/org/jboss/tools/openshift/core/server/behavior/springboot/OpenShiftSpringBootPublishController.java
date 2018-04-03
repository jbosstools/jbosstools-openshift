/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.util.RemotePath;
import org.jboss.tools.openshift.core.server.RSync;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

public class OpenShiftSpringBootPublishController extends OpenShiftPublishController {

	public OpenShiftSpringBootPublishController() {
		// keep for reflection instantiation
	}

	@Override
	protected RSync createRsync(final IServer server, final IProgressMonitor monitor) throws CoreException {
		return OpenShiftSpringBootPublishUtils.createRSync(server, monitor);
	}
	
	@Override
	protected File getDeploymentsRootFolder() throws CoreException {
		return new File(super.getDeploymentsRootFolder(), OpenShiftSpringBootPublishUtils.BASE_PATH);
	}

	@Override
	protected IPath getModuleDeployRoot(IModule[] module, boolean isBinaryObject) throws CoreException {
		if (module == null) {
			return null;
		}

		if (isRootModule(module)) {
			IModule rootModule = module[0];
			return super.getModuleDeployRoot(module, isBinaryObject)
					.append(OpenShiftSpringBootPublishUtils.getRootModuleDeployPath(rootModule));
		} else {
			IModule childModule = module[module.length - 1];
			return getChildModuleDeployPath(childModule);
		}
	}

	private IPath getChildModuleDeployPath(IModule module) throws CoreException {
		IPath localDestination = 
				new Path(getDeploymentOptions().getDeploymentsRootFolder(true))
					.append(OpenShiftSpringBootPublishUtils.getChildModuleDeployPath(module));
		localDestination.toFile().mkdirs();

		return new RemotePath(
				localDestination.toString(), 
				getDeploymentOptions().getPathSeparatorCharacter());
	}

	private boolean isRootModule(IModule[] module) {
		return module != null
				&& module.length == 1;
	}

	@Override
	protected boolean treatAsBinaryModule(IModule[] module) {
		return module.length == 1;
	}

	@Override
	protected boolean forceZipModule(IModule[] moduleTree) {
		return false;
	}
	
}
