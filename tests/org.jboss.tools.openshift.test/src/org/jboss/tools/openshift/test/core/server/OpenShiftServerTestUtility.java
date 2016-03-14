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
import org.eclipse.wst.server.core.ServerCore;

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
}
