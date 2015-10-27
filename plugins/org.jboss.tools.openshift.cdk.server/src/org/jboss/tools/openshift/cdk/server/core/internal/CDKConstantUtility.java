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
package org.jboss.tools.openshift.cdk.server.core.internal;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants;

// TODO - allow customization of this location
public class CDKConstantUtility {
	private static final String VAGRANT_LOCATION_LINUX = "/usr/bin/vagrant";

	public static String getVagrantLocation() {
		return VAGRANT_LOCATION_LINUX;
	}
	
	public static String getVagrantLocation(IServer server) {
		try {
			ILaunchConfiguration lc = server.getLaunchConfiguration(false, new NullProgressMonitor());
			if( lc != null ) {
				return lc.getAttribute(IExternalLaunchConstants.ATTR_LOCATION, getVagrantLocation());
			}
		} catch(CoreException ce) {
			// ignore, this is non-critical
		}
		return VAGRANT_LOCATION_LINUX;
	}
}
