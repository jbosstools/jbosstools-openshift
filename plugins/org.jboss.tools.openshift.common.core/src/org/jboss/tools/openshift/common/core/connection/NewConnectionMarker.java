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
 * A class that is used to have an entry "<New Connection>" in a combo that
 * shows all connections.
 * 
 * @author Andre Dietisheim
 * 
 */
public class NewConnectionMarker extends AbstractConnection {

	private static final NewConnectionMarker INSTANCE = new NewConnectionMarker();
	
	public static final NewConnectionMarker getInstance() {
		return INSTANCE;
	}
	
	private NewConnectionMarker() {
		super(null);
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
	}

	@Override
	public boolean connect() {
		return false;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public String getHost() {
		return "<New Connection>";
	}

	@Override
	public String getScheme() {
		return null;
	}
	
	@Override
	public boolean canConnect() {
		return false;
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public void setUsername(String username) {
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public void setPassword(String password) {
	}

	@Override
	public void setRememberPassword(boolean rememberPassword) {
	}

	@Override
	public boolean isRememberPassword() {
		return false;
	}

	@Override
	public IConnection clone() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void update(IConnection connection) {
	}

	@Override
	public void notifyUsage() {
	}
}
