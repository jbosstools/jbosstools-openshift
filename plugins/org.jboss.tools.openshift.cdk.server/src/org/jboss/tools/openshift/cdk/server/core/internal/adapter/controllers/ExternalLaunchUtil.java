/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IServer;

public class ExternalLaunchUtil implements IExternalLaunchConstants {

	public static ILaunchConfigurationWorkingCopy findExternalToolsLaunchConfig(IServer s, String launchName) throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(EXTERNAL_TOOLS);
		ILaunchConfiguration[] all = manager.getLaunchConfigurations(type);
		for( int i = 0; i < all.length; i++ ) {
			if( all[i].getName().equals(launchName)) {
				return all[i].getWorkingCopy();
			}
		}
		ILaunchConfigurationWorkingCopy wc = type.newInstance(null, launchName);
		return wc;
	}
}
