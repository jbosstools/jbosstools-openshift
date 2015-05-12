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
	 * Retrieve the connection for the given resource or null
	 * if its not found
	 * 
	 * @param resource
	 * @return a connection
	 */
	public static Connection getConnectionFor(IResource resource) {
		Collection<Connection> all = ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
		for (Connection connection : all) {
			if(connection.ownsResource(resource)) {
				return connection;
			}
		}
		return null;
	}
}
