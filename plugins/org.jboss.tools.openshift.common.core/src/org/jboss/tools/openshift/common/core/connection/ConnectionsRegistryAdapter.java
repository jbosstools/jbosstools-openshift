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

/**
 * Adapter to allow listners who only want to listen to specific events
 * 
 * @author jeff.cantrill
 *
 */
public class ConnectionsRegistryAdapter implements IConnectionsRegistryListener {

	@Override
	public void connectionAdded(IConnection connection) {
		// empty default implementation
	}

	@Override
	public void connectionRemoved(IConnection connection) {
		// empty default implementation
	}

	@Override
	public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
		// empty default implementation
	}

}
