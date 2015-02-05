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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.tools.openshift.common.core.utils.StringUtils;


/**
 * @author Andre Dietisheim
 */
public class ConnectionsFactory implements IConnectionsFactory {

	// TODO: replace by a concurrent map
	private Set<IConnectionFactory> connectionFactories = new CopyOnWriteArraySet<IConnectionFactory>();

	public ConnectionsFactory() {
	}

	@Override
	public IConnection create(String host) throws IOException {
		for (IConnectionFactory factory : connectionFactories) {
			IConnection connection = factory.create(host);
			if (connection.canConnect()) {
				return connection;
			}
		}
		return null;
	}
	
	public void addConnectionFactory(IConnectionFactory factory) {
		connectionFactories.add(factory);
	}

	public void removeConnectionFactory(IConnectionFactory factory) {
		connectionFactories.remove(factory);
	}

	@Override
	public IConnectionFactory getById(String id) {
		if (StringUtils.isEmpty(id)) {
			return null;
		}
		
		IConnectionFactory matchingFactory = null;
		for (IConnectionFactory factory : connectionFactories) {
			if (id.equals(factory.getId())) {
				matchingFactory = factory;
				break;
			}
		}
		return matchingFactory;
	}
	
	@Override
	public <T extends Class<? extends IConnection>> IConnectionFactory getByConnection(T clazz) {
		if (clazz == null) {
			return null;
		}
		
		IConnectionFactory matchingFactory = null;
		for (IConnectionFactory factory : connectionFactories) {
			if (factory != null
					&& factory.canCreate(clazz)) {
				matchingFactory = factory;
				break;
			}
		}
		return matchingFactory;
	}

	@Override
	public Collection<IConnectionFactory> getAll() {
		return new ArrayList<IConnectionFactory>(connectionFactories);
	}

}
