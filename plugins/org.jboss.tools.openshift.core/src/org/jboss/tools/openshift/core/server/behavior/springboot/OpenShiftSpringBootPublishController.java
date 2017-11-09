/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;

public class OpenShiftSpringBootPublishController extends OpenShiftPublishController {

	public OpenShiftSpringBootPublishController() {
		// keep for reflection instantiation
	}

	@Override
	public IStatus canPublish() {
		// TODO Check that the springboot-devtool mode is activated?
		return super.canPublish();
	}
	
	@Override
	protected boolean treatAsBinaryModule(IModule[] module) {
		return true;
	}
}
