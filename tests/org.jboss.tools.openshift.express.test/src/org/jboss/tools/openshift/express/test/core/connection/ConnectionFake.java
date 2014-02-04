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
package org.jboss.tools.openshift.express.test.core.connection;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.test.core.NoopUserFake;

/**
 * @author Andre Dietisheim
 */
public class ConnectionFake extends Connection {

	private static final String USERNAME = "testUser";
	private boolean authenticationTriggered;
	
	public ConnectionFake() {
		this(USERNAME);
	}
	
	public ConnectionFake(String username) {
		this(username, null);
	}

	public ConnectionFake(String username, String host) {
		super(username, null, host, false, null);
	}

	public ConnectionFake(String username, String scheme, String host) {
		super(username, null, scheme, host, false, null, null);
	}
	
	public void setConnected(boolean connected) {
		if (connected) {
			setUser(new NoopUserFake());
		} else {
			clearUser();
		}
	}
	
	@Override
	protected boolean createUser() {
		return this.authenticationTriggered = true;
	}

	public boolean isAuthenticationTriggered() {
		return authenticationTriggered;
	}
	
	@Override
	public void save() {
		// dont do anything
	}
}
