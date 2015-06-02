/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;


/**
 *   A listener that gets notified of changes (additions, removals, modifications) of connections contained within the connections model. 
 *   
 *   @author Rob Stryker
 *   @author Andre Dietisheim
 *   @author Jeff Cantrill
 */
public interface IConnectionsRegistryListener {
	
	/**
	 * Be alerted that a connection has been added
	 * @param connection
	 */
	public void connectionAdded(IConnection connection);
	
	/**
	 * Be alerted that a connection has been removed
	 * @param connection
	 */
	public void connectionRemoved(IConnection connection);
	
	/**
	 * Be alerted that a connection has been changed
	 * @param connection
	 * @param the name of the property
	 * @param the old value of the property
	 * @param the new value of the property
	 */
	public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue);
}
