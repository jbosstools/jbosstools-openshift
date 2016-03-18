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
package org.jboss.tools.openshift.test.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ServerProfileModel;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

public class OpenShiftServerTestUtility {

	public static void cleanup() {
		IServer[] all = ServerCore.getServers();
		for( int i = 0; i < all.length; i++ ) {
			try {
				all[i].delete();
			} catch(CoreException ce) {
				ce.printStackTrace();
			}
		}
	}
	
	
	public static IServer createOpenshift3Server(String name, String profile) throws CoreException {
		IServerType type = ServerCore.findServerType("org.jboss.tools.openshift.server.type");
		IServerWorkingCopy wc = type.createServer(name, null, null);
		OpenShiftServerUtils.updateServer(name, "http://www.example.com", "dummy", 
				"dummy", "dummy", "dummy", "dummy", "dummy", wc);
		if( profile != null ) {
			ServerProfileModel.setProfile(wc, profile);
		}
		return wc.save(false, null);
	}
}
