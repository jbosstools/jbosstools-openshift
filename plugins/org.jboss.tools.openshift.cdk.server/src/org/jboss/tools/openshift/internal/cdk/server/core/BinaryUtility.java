/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CDK32Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.CRC100Server;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.IExternalLaunchConstants;
import org.jboss.tools.openshift.internal.common.core.util.CommandLocationBinary;

public class BinaryUtility {
	public static final BinaryUtility VAGRANT_BINARY = new BinaryUtility(
			"vagrant", "/usr/bin/vagrant", 
			"C:\\HashiCorp\\Vagrant\\bin\\vagrant.exe", null);

	public static final BinaryUtility MINISHIFT_BINARY = new BinaryUtility(
			"minishift", "/usr/bin/minishift", 
			"C:\\minishift.exe", CDK32Server.MINISHIFT_FILE);

	public static final BinaryUtility CRC_BINARY = new BinaryUtility(
			"crc", "/usr/bin/crc", 
			"C:\\crc.exe", CRC100Server.PROPERTY_BINARY_FILE);

	
	
	private String binName;
	private String defaultLocLinux;
	private String defaultLocWin;
	private String serverPropertyKey;
	private CommandLocationBinary binary;

	public BinaryUtility(
			String binName,
			String defaultLocLinux, String defaultLocWin, 
			String serverPropertyKey) {
				this.binName = binName;
				this.defaultLocLinux = defaultLocLinux;
				this.defaultLocWin = defaultLocWin;
				this.serverPropertyKey = serverPropertyKey;
	}


	public String getLocation() {
		return findLocation();
	}

	public String getLocation(IServer server) {
		return getLocation(server, true, true, true);
	}
	public String getLocation(IServer server, boolean checkServerProperty, 
			boolean checkLaunch, boolean checkPath) {

		if (server != null) {
			if( checkServerProperty && serverPropertyKey != null ) {
				String crcBin = server.getAttribute(serverPropertyKey, (String) null);
				if (crcBin != null)
					return crcBin;
			}
			
			if( checkLaunch ) {
				try {
					ILaunchConfiguration lc = server.getLaunchConfiguration(false, new NullProgressMonitor());
					return getLocation(lc);
				} catch (CoreException ce) {
					// ignore, this is non-critical
				}
			}
			
		}
		if( checkPath )
			return findLocation();
		return null;
	}

	public String getLocation(ILaunchConfiguration lc) throws CoreException {
		if (lc != null) {
			String ret = lc.getAttribute(IExternalLaunchConstants.ATTR_LOCATION, (String) null);
			if (ret != null && new File(ret).exists())
				return ret;
		}
		return findLocation();
	}

	public String findLocation() {
		if (binary == null) {
			binary = new CommandLocationBinary(binName);
			binary.addPlatformLocation(Platform.OS_LINUX, defaultLocLinux);
			binary.addPlatformLocation(Platform.OS_WIN32, defaultLocWin);
			binary.setDefaultPlatform(Platform.OS_LINUX);
		}
		return binary.findLocation();
	}
}
