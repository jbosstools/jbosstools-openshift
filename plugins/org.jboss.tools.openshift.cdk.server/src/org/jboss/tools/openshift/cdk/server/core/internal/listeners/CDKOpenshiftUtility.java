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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
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


	/**
	 * Return the first connection that matches this server 
	 * @param server
	 * @param adb
	 * @return
	 */
	public IConnection findExistingOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		for(IConnection c : connections) {
			if( serverMatchesConnection(server, c, adb)) {
				return c;
			}
		}
		return null;
	}
	
	/**
	 * Return all existing openshift connections that match this server
	 * 
	 * @param server
	 * @param adb
	 * @return
	 */
	public IConnection[] findExistingOpenshiftConnections(IServer server, ServiceManagerEnvironment adb) {
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		ArrayList<IConnection> ret = new ArrayList<IConnection>();
		for(IConnection c : connections) {
			if( serverMatchesConnection(server, c, adb)) {
				ret.add(c);
			}
		}
		return (IConnection[]) ret.toArray(new IConnection[ret.size()]);
	}

	
	public boolean serverMatchesConnection(IServer server, IConnection c, ServiceManagerEnvironment adb) {
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		if( c.getType() == ConnectionType.Kubernetes) {
			String host = c.getHost();
			if( host.equals(soughtHost)) {
				return true;
			}
		}
		return false;
	}
	public MultiStatus serverConnectionMatchError(IServer server, IConnection c, ServiceManagerEnvironment adb) {
		MultiStatus ms = new MultiStatus(CDKCoreActivator.PLUGIN_ID, 0, "Server " + server.getName() + " fails to match openshift connection " + c.toString() + ".", null);
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		if( c.getType() != ConnectionType.Kubernetes) {
			ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Connection type is expected to be kubernetes."));
		} else {
			String host = c.getHost();
			if( !host.equals(soughtHost)) {
				ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "Host is " + host + " but is expected to be " + soughtHost));
			}
		}
		return ms;
	}

	
	public IConnection createOpenshiftConnection(ServiceManagerEnvironment env) {
		return createOpenshiftConnection(env, ConnectionsRegistrySingleton.getInstance());
	}
	
	public IConnection createOpenshiftConnection(ServiceManagerEnvironment env, ConnectionsRegistry registry) {
		
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
		if( authScheme != null && !authScheme.isEmpty()) {
			authScheme = new String(""+authScheme.charAt(0)).toUpperCase() + authScheme.substring(1);
			
		}
		
		((Connection)con).setAuthScheme(authScheme);
		((Connection)con).setUsername(username);
		if( password != null ) {
			((Connection)con).setPassword(password);
		}
		((Connection)con).setRememberPassword(true);

		String ocLoc = env.get(ServiceManagerEnvironmentLoader.OC_LOCATION_KEY); 
		if( ocLoc != null ) {
			((Connection)con).setExtendedProperty(ICommonAttributes.OC_LOCATION_KEY, ocLoc);
			((Connection)con).setExtendedProperty(ICommonAttributes.OC_OVERRIDE_KEY, true);
		}
		
		updateOpenshiftConnection(env, con, false);
		
		
		if( registry != null )
			registry.add(con);
		return con;
	}
	
	public void updateOpenshiftConnection(ServiceManagerEnvironment env, IConnection con) {
		updateOpenshiftConnection(env, con, true);
	}
	
	public void updateOpenshiftConnection(ServiceManagerEnvironment env, IConnection con, boolean fireUpdate) {
		String dockerReg = env.getDockerRegistry();
		((Connection)con).setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, dockerReg);
		if( fireUpdate) {
			ConnectionsRegistrySingleton.getInstance().update(con, con);
		}
	}
	
}
