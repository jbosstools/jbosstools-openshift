/******************************************************************************* 
 * Copyright (c) 2016-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.listeners;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;

public class CDKOpenshiftUtility {

	private static final String MINISHIFT_HOME_SUBDIR_OC = "oc";
	private static final String MINISHIFT_HOME_SUBDIR_CACHE = "cache";

	/**
	 * Return the first connection that matches this server 
	 * @param server
	 * @param adb
	 * @return
	 */
	public IConnection findExistingOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
		return findExistingOpenshiftConnection(adb.openshiftHost, adb.openshiftPort);
	}

	public IConnection findExistingOpenshiftConnection(String host, int port) {
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		for (IConnection c : connections) {
			if (serverMatchesConnection(c, host, port)) {
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
		return findExistingOpenshiftConnections(server, adb.openshiftHost, adb.openshiftPort);
	}

	public IConnection[] findExistingOpenshiftConnections(IServer server, 
			String host, int port) {
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		ArrayList<IConnection> ret = new ArrayList<>();
		for (IConnection c : connections) {
			if (serverMatchesConnection(c, host, port)) {
				ret.add(c);
			}
		}
		return ret.toArray(new IConnection[ret.size()]);
	}

	public boolean serverMatchesConnection(IConnection c, ServiceManagerEnvironment adb) {
		return serverMatchesConnection(c, adb.openshiftHost, adb.openshiftPort);
	}
	
	public boolean serverMatchesConnection(IConnection c, String host, int port) {
		String soughtHost = host + ":" + port;
		if (c.getType() == ConnectionType.Kubernetes) {
			String cHost = c.getHost();
			if (cHost.equals(soughtHost)) {
				return true;
			}
		}
		return false;
	}

	public MultiStatus serverConnectionMatchError(IServer server, IConnection c, ServiceManagerEnvironment adb) {
		MultiStatus ms = new MultiStatus(CDKCoreActivator.PLUGIN_ID, 0,
				"Server " + server.getName() + " fails to match openshift connection " + c.toString() + ".", null);
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		if (c.getType() != ConnectionType.Kubernetes) {
			ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID,
					"Connection type is expected to be kubernetes."));
		} else {
			String host = c.getHost();
			if (!host.equals(soughtHost)) {
				ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID,
						"Host is " + host + " but is expected to be " + soughtHost));
			}
		}
		return ms;
	}

	public IConnection createOpenshiftConnection(IServer server, ServiceManagerEnvironment env) {
		return createOpenshiftConnection(server, env, ConnectionsRegistrySingleton.getInstance());
	}

	public IConnection createOpenshiftConnection(IServer server, 
			ServiceManagerEnvironment env, ConnectionsRegistry registry) {
		String ocLocation = getOcLocation(env, server);
		String host = env.openshiftHost;
		int port = env.openshiftPort;
		String authType = env.getAuthorizationScheme();
		String username = env.getUsername();
		String password = env.getPassword();
		IConnection con = createOpenshiftConnection(host, port, 
				authType, username, password, ocLocation, registry);
		setDockerRegistry(env, con, false);
		return con;
	}

	public IConnection createOpenshiftConnection(String host, int port, String authType, 
			String username, String password, 
			String ocLocation, ConnectionsRegistry registry) {

		// Create the connection
		String soughtHost = host + ":" + port;
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
		IConnection con = factory.create(soughtHost);

		// Set some defaults
		authType = getOrDefaultAuthType(authType);
		((Connection) con).setAuthScheme(authType);
		((Connection) con).setUsername(username);
		if (password != null) {
			((Connection) con).setPassword(password);
		}
		((Connection) con).setRememberPassword(true);

		if (ocLocation != null) {
			((Connection) con).setExtendedProperty(ICommonAttributes.OC_LOCATION_KEY, ocLocation);
			((Connection) con).setExtendedProperty(ICommonAttributes.OC_OVERRIDE_KEY, true);
		}
		if (registry != null)
			registry.add(con);
		return con;
	}

	private String getOrDefaultAuthType(String suggestedAuthType) {
		if( suggestedAuthType == null || suggestedAuthType.isEmpty() || suggestedAuthType.length() <= 1) {
			return "Basic";
		}
		return Character.toUpperCase(suggestedAuthType.charAt(0)) + suggestedAuthType.substring(1);
	}
	
	private String getOcLocation(ServiceManagerEnvironment env, IServer server) {
		String ocLocation = env.get(ServiceManagerEnvironmentLoader.OC_LOCATION_KEY);
		if (ocLocation == null) {
			ocLocation = findOCInMinishiftHome(server);
		}
		return ocLocation;
	}

	private String findOCInMinishiftHome(IServer server) {
		String ocLocation = null;
		String minishiftHome = CDKServerUtility.getMinishiftHomeOrDefault(server);
		if (StringUtils.isEmpty(minishiftHome)
				|| !new File(minishiftHome).exists()) {
			return null;
		}
		
		Path miniShifthomePath = Paths.get(minishiftHome, MINISHIFT_HOME_SUBDIR_CACHE, MINISHIFT_HOME_SUBDIR_OC);
		RecursiveExecutableFinder finder = new RecursiveExecutableFinder();
		try {
			Files.walkFileTree(miniShifthomePath, finder);
			ocLocation = finder.getOCLocation();
		} catch (IOException e) {
			// ignore
		}
		return ocLocation;
	}

	public void setDockerRegistry(ServiceManagerEnvironment env, IConnection con) {
		setDockerRegistry(env, con, true);
	}

	public void setDockerRegistry(ServiceManagerEnvironment env, IConnection con, boolean fireUpdate) {
		String dockerReg = env.getDockerRegistry();
		((Connection) con).setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, dockerReg);
		if (fireUpdate) {
			ConnectionsRegistrySingleton.getInstance().update(con, con);
		}
	}

	private class RecursiveExecutableFinder extends SimpleFileVisitor<Path> {

		private String executable = null;

		@Override
	    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	        throws IOException {
		        Objects.requireNonNull(file);
		        Objects.requireNonNull(attrs);

		        if (Files.isExecutable(file)) {
					this.executable = file.toAbsolutePath().toString();
					return FileVisitResult.TERMINATE;
		        } else {
					return FileVisitResult.CONTINUE;
		        }
	    }

		public String getOCLocation() {
			return executable;
		}
	}

}
