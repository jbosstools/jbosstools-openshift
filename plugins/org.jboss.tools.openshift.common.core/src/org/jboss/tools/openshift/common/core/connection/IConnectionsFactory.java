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
package org.jboss.tools.openshift.common.core.connection;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Andre Dietisheim
 */
public interface IConnectionsFactory {

	/** connection factory for OpenShift 2 */
	public static final String CONNECTIONFACTORY_EXPRESS_ID = "org.jboss.tools.openshift.express.core.ConnectionFactory";

	/** connection factory for OpenShift 3 */
	public static final String CONNECTIONFACTORY_OPENSHIFT_ID = "org.jboss.tools.openshift.core.ConnectionFactory";
	
	/**
	 * Creates a connections for the given host. Queries the connection
	 * factories that are registered and returns the first one that can create a
	 * connection. Returns <code>null</code> otherwise
	 * 
	 * @param host
	 * @return
	 * @throws IOException
	 */
	public IConnection create(String host) throws IOException;

	/**
	 * Returns a connection factory that can create connections for the given host.
	 * 
	 * @param host the host to get a factory for
	 * @return the connection factory
	 * @throws IOException
	 */
	public IConnectionFactory getFactory(String host) throws IOException;
	
	/**
	 * Returns all connection factories that are registered to this composite
	 * factory.
	 * 
	 * @return
	 */
	public Collection<IConnectionFactory> getAll();
	
	/**
	 * Returns the connection factory that is registered to this composite
	 * factory for the given id. Returns <code>null</code> otherwise.
	 * 
	 * @param id
	 * @return
	 */
	public IConnectionFactory getById(String id);

	/**
	 * Returns a factory that is capable of creating a connection that can connect
	 * @param host
	 * @return
	 * @throws IOException
	 */
	public <T extends IConnection> IConnectionFactory getByConnection(Class<T> clazz);

}
