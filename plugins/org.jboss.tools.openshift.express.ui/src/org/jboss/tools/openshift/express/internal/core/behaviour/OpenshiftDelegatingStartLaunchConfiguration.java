/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.jboss.ide.eclipse.as.core.server.internal.launch.DelegatingStartLaunchConfiguration;

public class OpenshiftDelegatingStartLaunchConfiguration extends
		DelegatingStartLaunchConfiguration implements
		ILaunchConfigurationDelegate {

}
