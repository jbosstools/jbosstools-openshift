/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.core;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 *   A listener for the connections model. 
 */
public interface IConnectionsModelListener {
	
	/**
	 * Be alerted that a connection has been added
	 * @param connection
	 */
	public void connectionAdded(Connection connection);
	
	/**
	 * Be alerted that a connection has been removed
	 * @param connection
	 */
	public void connectionRemoved(Connection connection);
	
	/**
	 * Be alerted that a connection has been changed
	 * @param connection
	 */
	public void connectionChanged(Connection connection);
}
