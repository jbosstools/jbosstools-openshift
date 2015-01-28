/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;


/**
 * A class that is used to have an entry <New ExpressConnection> in a combo view with
 * connections.
 * 
 * @author Andre Dietisheim
 * 
 */
public class NewConnectionMarker extends AbstractConnection {

	public NewConnectionMarker() {
		super("<New Connection>");
	}

	@Override
	public int hashCode() {
		return getHost().hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		return object instanceof NewConnectionMarker;
	}
	
	@Override
	public boolean isDefaultHost() {
		return false;
	}

	@Override
	public ConnectionType getType() {
		return null;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean connect() {
		return false;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

}
