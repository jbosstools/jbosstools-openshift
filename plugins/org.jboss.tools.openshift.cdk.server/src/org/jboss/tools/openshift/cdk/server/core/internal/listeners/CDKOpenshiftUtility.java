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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.core.connection.Connection;

public class CDKOpenshiftUtility {
	private static String DOTCDK_AUTH_SCHEME = "openshift.auth.scheme";
	private static String DOTCDK_AUTH_USERNAME = "openshift.auth.username";
	private static String DOTCDK_AUTH_PASS = "openshift.auth.password";
	


	public IConnection findExistingOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
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
	
	public IConnection createOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
		return createOpenshiftConnection(server, adb, ConnectionsRegistrySingleton.getInstance());
	}

	private static final String defaultPassword = "devel";
	
	public IConnection createOpenshiftConnection(IServer server, ServiceManagerEnvironment adb, ConnectionsRegistry registry) {
		Properties dotcdkProps = new CDKServerUtility().getDotCDK(server);
		String authScheme = dotcdkProps.containsKey(DOTCDK_AUTH_SCHEME) ? dotcdkProps.getProperty(DOTCDK_AUTH_SCHEME) : "Basic";
		String username = dotcdkProps.containsKey(DOTCDK_AUTH_USERNAME) ? dotcdkProps.getProperty(DOTCDK_AUTH_USERNAME) : "openshift-dev";
		
		String password = null;
		if( dotcdkProps.containsKey(DOTCDK_AUTH_PASS) ) {
			password = dotcdkProps.getProperty(DOTCDK_AUTH_PASS);
		} else {
			// If the .cdk file only has a username set and no password, we don't want to set a password.  
			// We can assume the user hand-modified hte username and so maybe has their own custom password they want to enter by hand
			if( dotcdkProps.containsKey(DOTCDK_AUTH_USERNAME) ) {
				// .cdk file set a username and no password.  leave as null
			} else {
				// .cdk did not set a username OR password... so we will set the default password to devel
				password = defaultPassword;
			}
		}
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
		IConnection con = factory.create(soughtHost);
		((Connection)con).setAuthScheme(authScheme);
		((Connection)con).setUsername(username);

		if( password != null ) {
			((Connection)con).setPassword(password);
			if(defaultPassword.equals(password) //remember it, user does not know it 
				|| isSavePassword(username)) {
				((Connection)con).setRememberPassword(true);
			}
		}
		
		if( registry != null )
			registry.add(con);
		return con;
	}

	/**
	 * Check if user selected save password at creating CDK server and extend that choice
	 * to remembering password of openshift connection.
	 * 
	 * @param username
	 * @return
	 */
	private boolean isSavePassword(String username) {
		ICredentialDomain domain = CredentialService.getCredentialModel().getDomain(CredentialService.REDHAT_ACCESS);
		if(domain == null) {
			return false; //should not happen
		}
		try {
			Method method = domain.getClass().getDeclaredMethod("userRequiresPrompt", new Class[]{String.class});
			method.setAccessible(true);
			Object result = method.invoke(domain, new Object[]{username});
			return result instanceof Boolean && !((Boolean)result).booleanValue();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			CDKCoreActivator.pluginLog().logError(e);
		}
		return false;
	}
	
}
