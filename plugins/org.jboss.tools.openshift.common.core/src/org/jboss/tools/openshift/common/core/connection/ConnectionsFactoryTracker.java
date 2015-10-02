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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Andre Dietisheim
 */
public class ConnectionsFactoryTracker extends ServiceTracker<IConnectionsFactory, IConnectionsFactory> implements IConnectionsFactory {

	public ConnectionsFactoryTracker() {
		super(OpenShiftCommonCoreActivator.getBundleContext(), IConnectionsFactory.class, null);
	}

	@Override
	public IConnection create(String host) throws IOException {
		IConnectionsFactory service = getService();
		if (service == null) {
			return null;
		}

		return service.create(host);
	}

	@Override
	public IConnectionFactory getFactory(String host) throws IOException {
		IConnectionsFactory service = getService();
		if (service == null) {
			return null;
		}

		return service.getFactory(host);
	}

	@Override
	public IConnectionFactory getById(String id) {
		IConnectionsFactory service = getService();
		if (service == null) {
			return null;
		}

		return service.getById(id);
	}

	@Override
	public Collection<IConnectionFactory> getAll() {
		IConnectionsFactory service = getService();
		if (service == null) {
			return null;
		}

		return service.getAll(); 
	}

	@Override
	public <T extends IConnection> Collection<IConnectionFactory> getAll(Class<T> clazz) {
		if (clazz == null) {
			return getAll();
		} else {
			return Arrays.asList(getByConnection(clazz));
		}
	}

	@Override
	public <T extends IConnection> IConnectionFactory getByConnection(Class<T> clazz) {
		IConnectionsFactory service = getService();
		if (service == null) {
			return null;
		}

		return service.getByConnection(clazz);	
	}

}
