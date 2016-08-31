/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

import java.util.Collection;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;

import com.openshift.restclient.model.IResource;

/**
 * Utility class to augment the connections registry
 * for OpenShift Connections
 * 
 * @author jeff.cantrill
 *
 */
public class ConnectionsRegistryUtil {
	
	private ConnectionsRegistryUtil() {
	}
	
	/**
	 * Retrieve the connection for the given resource
	 * 
	 * @param resource
	 * @return a connection
	 * @throws ConnectionNotFoundException  if the connection can't be found
	 */
	public static Connection getConnectionFor(IResource resource) {
		if (resource == null) {
			return null;
		}
		Connection connection = safeGetConnectionFor(resource);
		if(connection == null) {
			throw new ConnectionNotFoundException(resource);
		}
		return connection;
	}
	
	/**
	 * Retrieve the connection for the given resources
	 * @param resource
	 * @return the connection or null if not found
	 */
	public static Connection safeGetConnectionFor(IResource resource) {
		Collection<Connection> all = ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
		for (Connection connection : all) {
			if(connection.ownsResource(resource)) {
				return connection;
			}
		}
		return null;
	}

}
