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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftUnknonwSSHKeyTypeException;

/**
 * @author Andre Dietisheim
 */
public class ConnectionFake extends Connection {

	private static final String USERNAME = "testUser";
	private boolean authenticationTriggered;
	
	public ConnectionFake() {
		this(USERNAME);
	}
	
	public ConnectionFake(URL url) throws UnsupportedEncodingException {
		super(url, null);
	}

	public ConnectionFake(String username) {
		this(username, null);
	}

	public ConnectionFake(String username, String host) {
		super(username, null, host, false, null);
	}


	public void setConnected(boolean connected) {
		if (connected) {
			setUser(new UserFake());
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

	private static class UserFake implements IUser {

		@Override
		public String getCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasCreationLog() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void refresh() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRhlogin() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPassword() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getServer() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getAuthKey() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getAuthIV() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IOpenShiftConnection getConnection() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDomain createDomain(String id) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IDomain> getDomains() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDomain getDefaultDomain() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IDomain getDomain(String id) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasDomain() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasDomain(String id) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<IOpenShiftSSHKey> getSSHKeys() throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IOpenShiftSSHKey getSSHKeyByName(String name) throws OpenShiftUnknonwSSHKeyTypeException,
				OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException,
				OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasSSHKeyName(String name) throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasSSHPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteKey(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getMaxGears() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getConsumedGears() {
			throw new UnsupportedOperationException();
		}
		
	}
}
