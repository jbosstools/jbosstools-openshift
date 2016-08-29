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

public class CDKConstantUtility {
	private static final String SCRIPT_LOCATION_LINUX = "/usr/bin/script";
	private static final String VAGRANT_LOCATION_LINUX = "/usr/bin/vagrant";
	
	// Seems weird?  See https://github.com/mitchellh/vagrant/issues/1652
	private static final String VAGRANT_LOCATION_WINDOWS = "C:\\HashiCorp\\Vagrant\\bin\\vagrant.exe";
	
	private static CommandLocationBinary vagrantBinary;
	private static CommandLocationBinary scriptBinary;
	
	public static String getVagrantLocation() {
		return findVagrantLocation();
	}
	
	public static String getVagrantLocation(IServer server) {
		if( server != null ) {
			try {
				ILaunchConfiguration lc = server.getLaunchConfiguration(false, new NullProgressMonitor());
				if( lc != null ) {
					String ret = lc.getAttribute(IExternalLaunchConstants.ATTR_LOCATION, (String)null);
					if( ret != null && new File(ret).exists())
							return ret;
				}
			} catch(CoreException ce) {
				// ignore, this is non-critical
			}
		}
		return findVagrantLocation();
	}
	
	private static String findVagrantLocation() {
		if( vagrantBinary == null ) {
			vagrantBinary = new CommandLocationBinary("vagrant");
			vagrantBinary.addPlatformLocation(Platform.OS_LINUX, VAGRANT_LOCATION_LINUX);
			vagrantBinary.addPlatformLocation(Platform.OS_WIN32, VAGRANT_LOCATION_WINDOWS);
			vagrantBinary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return vagrantBinary.findLocation();
	}
	
	public static String getScriptLocation() {
		if( scriptBinary == null ) {
			scriptBinary = new CommandLocationBinary("script");
			scriptBinary.addPlatformLocation(Platform.OS_LINUX, SCRIPT_LOCATION_LINUX);
			scriptBinary.addPlatformLocation(Platform.OS_MACOSX, SCRIPT_LOCATION_LINUX);
			scriptBinary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return scriptBinary.findLocation();
	}

	
}
