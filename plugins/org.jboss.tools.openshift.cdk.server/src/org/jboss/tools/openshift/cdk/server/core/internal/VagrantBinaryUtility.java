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

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;

public class VagrantBinaryUtility {
	private static final String VAGRANT_LOCATION_LINUX = "/usr/bin/vagrant";
	
	// Seems weird?  See https://github.com/mitchellh/vagrant/issues/1652
	private static final String VAGRANT_LOCATION_WINDOWS = "C:\\HashiCorp\\Vagrant\\bin\\vagrant.exe";
	
	private static CommandLocationBinary binary;
	
	public static String getVagrantLocation() {
		return findVagrantLocation();
	}
	
	public static String getVagrantLocation(IServer server) {
		if( server != null ) {
			try {
				ILaunchConfiguration lc = server.getLaunchConfiguration(false, new NullProgressMonitor());
				return getVagrantLocation(lc);
			} catch(CoreException ce) {
				// ignore, this is non-critical
			}
		}
		return findVagrantLocation();
	}
	
	public static String getVagrantLocation(ILaunchConfiguration lc) throws CoreException {
		if( lc != null ) {
			String ret = lc.getAttribute(IExternalLaunchConstants.ATTR_LOCATION, (String)null);
			if( ret != null && new File(ret).exists())
					return ret;
		}
		return findVagrantLocation();
	}
	
	private static String findVagrantLocation() {
		if( binary == null ) {
			binary = new CommandLocationBinary("vagrant");
			binary.addPlatformLocation(Platform.OS_LINUX, VAGRANT_LOCATION_LINUX);
			binary.addPlatformLocation(Platform.OS_WIN32, VAGRANT_LOCATION_WINDOWS);
			binary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return binary.findLocation();
	}
}
