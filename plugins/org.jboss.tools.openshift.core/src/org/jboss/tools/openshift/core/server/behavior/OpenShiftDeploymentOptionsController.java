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

import org.jboss.ide.eclipse.as.core.util.ServerUtil;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.AbstractSubsystemController;
import org.jboss.tools.as.core.server.controllable.systems.IDeploymentOptionsController;

public class OpenShiftDeploymentOptionsController extends AbstractSubsystemController implements IDeploymentOptionsController {

	public OpenShiftDeploymentOptionsController() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public char getPathSeparatorCharacter() {
		return File.separatorChar;
	}

	@Override
	public String getDeploymentsRootFolder(boolean absolute) {
		return ServerUtil.getServerStateLocation(getServer()).append("deploy").toFile().getAbsolutePath();
	}

	@Override
	public String getDeploymentsTemporaryFolder(boolean absolute) {
		return ServerUtil.getServerStateLocation(getServer()).append("tempDeploy").toFile().getAbsolutePath();
	}

	@Override
	public boolean prefersZippedDeployments() {
		return false;
	}

}
