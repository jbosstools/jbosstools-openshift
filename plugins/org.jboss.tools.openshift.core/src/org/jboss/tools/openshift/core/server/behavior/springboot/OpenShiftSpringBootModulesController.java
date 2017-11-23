/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.behavior.eap.OpenShiftEapModulesController;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftSpringBootModulesController extends OpenShiftEapModulesController
		implements ISubsystemController {

	@Override
	public int changeModuleStateTo(IModule[] module, int state, IProgressMonitor monitor) throws CoreException {
		//		syncDown(monitor);
		syncUp(monitor);
		return state;
	}
}
