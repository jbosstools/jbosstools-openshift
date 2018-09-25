/******************************************************************************* 
 * Copyright (c) 2016-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.listeners;

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
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;

public class CDKOpenshiftUtility {

	private static final String MINISHIFT_HOME_SUBDIR_OC = "oc";
	private static final String MINISHIFT_HOME_SUBDIR_CACHE = "cache";
	private static final String ENV_VAR_MINISHIFT_HOME = "minishift.home.location";

	/**
	 * Return the first connection that matches this server 
	 * @param server
	 * @param adb
	 * @return
	 */
	public IConnection findExistingOpenshiftConnection(IServer server, ServiceManagerEnvironment adb) {
		Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
		for (IConnection c : connections) {
			if (serverMatchesConnection(server, c, adb)) {
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
		ArrayList<IConnection> ret = new ArrayList<>();
		for (IConnection c : connections) {
			if (serverMatchesConnection(server, c, adb)) {
				ret.add(c);
			}
		}
		return ret.toArray(new IConnection[ret.size()]);
	}

	public boolean serverMatchesConnection(IServer server, IConnection c, ServiceManagerEnvironment adb) {
		String soughtHost = adb.openshiftHost + ":" + adb.openshiftPort;
		if (c.getType() == ConnectionType.Kubernetes) {
			String host = c.getHost();
			if (host.equals(soughtHost)) {
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
		if (authScheme != null && !authScheme.isEmpty()) {
			authScheme = new String("" + authScheme.charAt(0)).toUpperCase() + authScheme.substring(1);

		}

		((Connection) con).setAuthScheme(authScheme);
		((Connection) con).setUsername(username);
		if (password != null) {
			((Connection) con).setPassword(password);
		}
		((Connection) con).setRememberPassword(true);

		setOcLocation(env, con, server);

		updateOpenshiftConnection(env, con, false);

		if (registry != null)
			registry.add(con);
		return con;
	}

	private void setOcLocation(ServiceManagerEnvironment env, IConnection con, IServer server) {
		String ocLocation = env.get(ServiceManagerEnvironmentLoader.OC_LOCATION_KEY);
		if (ocLocation == null) {
			ocLocation = findOCInMinishiftHome(server);
		}
		if (ocLocation != null) {
			((Connection) con).setExtendedProperty(ICommonAttributes.OC_LOCATION_KEY, ocLocation);
			((Connection) con).setExtendedProperty(ICommonAttributes.OC_OVERRIDE_KEY, true);
		}
	}

	private String findOCInMinishiftHome(IServer server) {
		String ocLocation = null;
		String minishiftHome = server.getAttribute(ENV_VAR_MINISHIFT_HOME, (String) null);
		if (minishiftHome == null) {
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

	public void updateOpenshiftConnection(ServiceManagerEnvironment env, IConnection con) {
		updateOpenshiftConnection(env, con, true);
	}

	public void updateOpenshiftConnection(ServiceManagerEnvironment env, IConnection con, boolean fireUpdate) {
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
