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
package org.jboss.tools.openshift.express.test.core;

import java.util.Collection;
import java.util.List;

import com.openshift.client.IAuthorization;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftUnknonwSSHKeyTypeException;

/**
 * @author Andre Dietisheim
 */
public class NoopUserFake implements IUser {

	@Override
	public void refresh() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasCreationLog() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCreationLog() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOpenShiftSSHKey putSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasSSHPublicKey(String publicKey) throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasSSHKeyName(String name) throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasDomain(String id) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasDomain() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getServer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IOpenShiftSSHKey> getSSHKeys() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOpenShiftSSHKey getSSHKeyByPublicKey(String publicKey)
			throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOpenShiftSSHKey getSSHKeyByName(String name)
			throws OpenShiftUnknonwSSHKeyTypeException, OpenShiftException {
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
	public int getMaxGears() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IDomain> getDomains() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDomain getDomain(String id) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDomain getDefaultDomain() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getConsumedGears() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOpenShiftConnection getConnection() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteKey(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDomain createDomain(String id) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Messages getMessages() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOpenShiftSSHKey addSSHKey(String name, ISSHPublicKey key) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeSSHKey(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IAuthorization createAuthorization(String arg0, String arg1) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IAuthorization createAuthorization(String arg0, String arg1, int arg2) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IAuthorization getAuthorization() throws OpenShiftException {
		return null;
	}

	@Override
	public IAuthorization getAuthorization(String arg0) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<IAuthorization> getAuthorizations() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAuthorization(String arg0) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}
}
