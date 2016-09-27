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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.IExternalLaunchConstants;

public class CDKLaunchEnvironmentUtil {

	public static Map<String, String> createEnvironment(IServer server) {
		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		boolean passCredentials = cdkServer.passCredentials();
		String pass = null;
		if( passCredentials) {
			try {
				pass = cdkServer.getPassword();
			} catch (UsernameChangedException uce) {
				return createEnvironment(server, uce.getPassword(), uce.getUser());
			}
		}
		return createEnvironment(server, pass);
	}

	public static Map<String, String> createEnvironment(IServer server, String password) {
		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		return createEnvironment(server, password, cdkServer.getUsername());
	}

	public static Map<String, String> createEnvironment(IServer server, String password, String user) {
		Map<String, String> launchEnv = null;
		try {
			ILaunchConfiguration wc = server.getLaunchConfiguration(false, new NullProgressMonitor());
			if( wc != null )
				launchEnv = wc.getAttribute(IExternalLaunchConstants.ENVIRONMENT_VARS_KEY, (Map<String, String>) null);
		} catch (CoreException ce) {
			CDKCoreActivator.pluginLog().logWarning(
					"Unable to load environment for vagrant status call. System environment will be used instead.");
		}
		HashMap<String, String> systemEnv = new HashMap<>(System.getenv());

		if (launchEnv != null) {
			Iterator<String> it = launchEnv.keySet().iterator();
			String k = null;
			while (it.hasNext()) {
				k = it.next();
				systemEnv.put(k, launchEnv.get(k));
			}
		}

		CDKServer cdkServer = (CDKServer) server.loadAdapter(CDKServer.class, new NullProgressMonitor());
		boolean passCredentials = cdkServer.passCredentials();
		String userKey = cdkServer.getUserEnvironmentKey();
		String passKey = cdkServer.getPasswordEnvironmentKey();
		if (passCredentials) {
			systemEnv.put(userKey, user);
			systemEnv.put(passKey, password);
		} else {
			systemEnv.remove(userKey);
			systemEnv.remove(passKey);
		}
		return systemEnv;
	}

}
