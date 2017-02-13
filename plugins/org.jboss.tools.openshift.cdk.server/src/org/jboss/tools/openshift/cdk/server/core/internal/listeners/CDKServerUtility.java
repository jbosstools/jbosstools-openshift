/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDKServerUtility {

	public static Properties getDotCDK(IServer server) {
		String cdkFolder = server.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( cdkFolder != null && new File(cdkFolder).exists()) {
			return getDotCDK(cdkFolder);
		}
		return new Properties();
	}
	
	public static Properties getDotCDK(String cdkFolder) {
		return getDotCDK(new File(cdkFolder, ".cdk"));
	}
	
	public static Properties getDotCDK(String cdkFolder, String name) {
		return getDotCDK(new File(cdkFolder, name));
	}

	
	public static Properties getDotCDK(File dotcdk) {
		if( dotcdk.exists()) {
			try {
				Properties props = new Properties();
				props.load(new FileInputStream(dotcdk));
				return props;
			} catch(IOException ioe) {
				CDKCoreActivator.pluginLog().logError("Error loading properties from .cdk file " + dotcdk.getAbsolutePath(), ioe);
			}
		}
		return new Properties();
	}


	public static File getWorkingDirectory(IServer s) {
		String str = s.getAttribute(CDKServer.PROP_FOLDER, (String)null);
		if( str != null && new File(str).exists()) {
			return new File(str);
		}
		return null;
	}
	
}
