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

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class NoopOpenShiftConnectionFake implements IOpenShiftConnection {

	@Override
	public void setProxySet(boolean proxySet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProxyPort(String proxyPort) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProxyHost(String proxyHost) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setEnableSSLCertChecks(boolean doSSLChecks) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IUser getUser() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ICartridge> getStandaloneCartridges() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ExecutorService getExecutorService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IDomain> getDomains() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getServer() {
		throw new UnsupportedOperationException();
	}
}
