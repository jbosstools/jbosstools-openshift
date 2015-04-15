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
package org.jboss.tools.openshift.express.test.core.connection;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IQuickstart;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class NoopOpenShiftConnectionFake implements IOpenShiftConnection {

	@Override
	public IUser getUser() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IStandaloneCartridge> getStandaloneCartridges(boolean isObsolete) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<IStandaloneCartridge> getStandaloneCartridges() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ExecutorService getExecutorService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IEmbeddableCartridge> getEmbeddableCartridges(boolean includeObsolete) throws OpenShiftException {
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

	@Override
	public List<ICartridge> getCartridges(boolean includeObsolete) throws OpenShiftException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<ICartridge> getCartridges() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IQuickstart> getQuickstarts() throws OpenShiftException {
		throw new UnsupportedOperationException();
	}
}
