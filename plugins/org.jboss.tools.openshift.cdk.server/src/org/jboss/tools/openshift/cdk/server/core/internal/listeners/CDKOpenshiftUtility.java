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

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;

public class CDKOpenshiftUtility {


	public IConnection findExistingOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
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
	
	public IConnection createOpenshiftConnection(IServer server, ServiceManagerEnvironment env) {
		return createOpenshiftConnection(server, env, ConnectionsRegistrySingleton.getInstance());
	}
	
	public IConnection createOpenshiftConnection(IServer server, ServiceManagerEnvironment env, ConnectionsRegistry registry) {
		
		// Create the connection
		String soughtHost = env.openshiftHost + ":" + env.openshiftPort;
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
		IConnection con = factory.create(soughtHost);
		
		
		// Set some defaults
		String authScheme = env.getAuthorizationScheme(); 
		String username = env.getUsername();
		String password = env.getPassword();
		
		((Connection)con).setAuthScheme(authScheme);
		((Connection)con).setUsername(username);
		if( password != null ) {
			((Connection)con).setPassword(password);
		}
		((Connection)con).setRememberPassword(true);
		
		updateOpenshiftConnection(server, env, con, false);
		
		
		if( registry != null )
			registry.add(con);
		return con;
	}
	
	public void updateOpenshiftConnection(IServer server, ServiceManagerEnvironment env, IConnection con) {
		updateOpenshiftConnection(server, env, con, true);
	}
	
	public void updateOpenshiftConnection(IServer server, ServiceManagerEnvironment env, IConnection con, boolean fireUpdate) {
		String dockerReg = env.getDockerRegistry();
		((Connection)con).setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, dockerReg);
		if( fireUpdate) {
			ConnectionsRegistrySingleton.getInstance().update(con, con);
		}
	}
	
}
