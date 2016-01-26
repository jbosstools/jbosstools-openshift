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

import java.util.Collection;
import java.util.Properties;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.core.connection.Connection;

public class CDKOpenshiftUtility {
	private static String DOTCDK_AUTH_SCHEME = "openshift.auth.scheme";
	private static String DOTCDK_AUTH_USERNAME = "openshift.auth.username";


	public IConnection findExistingOpenshiftConnection(IServer server, ADBInfo adb) {
		Properties dotcdkProps = new CDKServerUtility().getDotCDK(server);
		String authScheme = dotcdkProps.containsKey(DOTCDK_AUTH_SCHEME) ? dotcdkProps.getProperty(DOTCDK_AUTH_SCHEME) : "Basic";
		String username = dotcdkProps.containsKey(DOTCDK_AUTH_USERNAME) ? dotcdkProps.getProperty(DOTCDK_AUTH_USERNAME) : "test-admin";
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		for(IConnection c : connections) {
			if( c.getType() == ConnectionType.Kubernetes) {
				String host = c.getHost();
				if( host.equals(soughtHost)) {
					return c;
				}
			}
		}
		return null;
	}
	
	public IConnection createOpenshiftConnection(IServer server, ADBInfo adb) {
		Properties dotcdkProps = new CDKServerUtility().getDotCDK(server);
		String authScheme = dotcdkProps.containsKey(DOTCDK_AUTH_SCHEME) ? dotcdkProps.getProperty(DOTCDK_AUTH_SCHEME) : "Basic";
		String username = dotcdkProps.containsKey(DOTCDK_AUTH_USERNAME) ? dotcdkProps.getProperty(DOTCDK_AUTH_USERNAME) : "test-admin";
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
		IConnection con = factory.create(soughtHost);
		((Connection)con).setAuthScheme(authScheme);
		((Connection)con).setUsername(username);
		ConnectionsRegistrySingleton.getInstance().add(con);
		return con;
	}
	
}
