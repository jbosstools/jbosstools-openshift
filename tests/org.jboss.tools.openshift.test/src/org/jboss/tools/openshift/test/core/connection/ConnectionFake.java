/*******************************************************************************
 * Copyright (c) 2012-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import java.io.IOException;

import org.jboss.tools.openshift.common.core.connection.AbstractConnection;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * @author Andre Dietisheim
 */
public class ConnectionFake extends AbstractConnection {

	private String username;
	private String password;
	
	ConnectionFake(String host) {
		super(host);
	}

	@Override
	public boolean connect() {
		return false;
	}

	@Override
	public boolean canConnect() throws IOException {
		return false;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		firePropertyChange(PROPERTY_USERNAME, this.username, this.username = username);
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
	}

	@Override
	public void enablePromptCredentials(boolean enable) {
	}

	@Override
	public boolean isRememberPassword() {
		return false;
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
	public boolean isConnected() {
		return false;
	}
	
	@Override
	public IConnection clone() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void update(IConnection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRememberPassword(boolean rememberPassword) {
	}

	@Override
	public void notifyUsage() {
	}
}