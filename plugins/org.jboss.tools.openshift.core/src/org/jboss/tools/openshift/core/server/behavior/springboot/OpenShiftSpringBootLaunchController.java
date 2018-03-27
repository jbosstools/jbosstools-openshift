/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior.springboot;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftLaunchController;
import org.jboss.tools.openshift.internal.core.server.debug.DebugContext;
import org.jboss.tools.openshift.internal.core.server.debug.OpenShiftDebugMode;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftSpringBootLaunchController extends OpenShiftLaunchController {

	@Override
	protected void setMode(String mode, DebugContext context, OpenShiftServerBehaviour beh, IProgressMonitor monitor) {
		super.setMode(mode, context, beh, monitor);
		new OpenShiftDebugMode(context).putEnvVar("JAVA_CLASSPATH", "/deployments/BOOT-INF/lib");
	}


}
