/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

public class OpenshiftUIModel extends AbstractOpenshiftUIModel<ConnectionsRegistry, OpenshiftUIModel> {

	protected static class OpenshiftUIModelSingletonHolder {
		public static final OpenshiftUIModel INSTANCE = new OpenshiftUIModel(
				ConnectionsRegistrySingleton.getInstance());
	}

	public static OpenshiftUIModel getInstance() {
		return OpenshiftUIModelSingletonHolder.INSTANCE;
	}

	private Map<IOpenShiftConnection, ConnectionWrapper> connections = new HashMap<>();

	private IConnectionsRegistryListener listener;

	/**
	 * Explicitly call this method, only for testing purposes!
	 */
	protected OpenshiftUIModel(ConnectionsRegistry registry) {
		super(null, registry);
		listener = new IConnectionsRegistryListener() {

			@Override
			public void connectionRemoved(IConnection connection) {
				removeConnection(connection);
				fireChanged(OpenshiftUIModel.this);
			}

			@Override
			public void connectionChanged(IConnection c, String property, Object oldValue, Object newValue) {
				ConnectionWrapper connection = connections.get(c);
				if (connection == null) {
					return;
				}
				connection.connectionChanged(property, oldValue, newValue);
			}

			@Override
			public void connectionAdded(IConnection connection) {
				if (!(connection instanceof IOpenShiftConnection)) {
					return;
				}
				addConnection(connection);
				fireChanged(OpenshiftUIModel.this);
			}
		};

		Collection<Connection> allConnections = registry.getAll(Connection.class);
		synchronized (connections) {
			for (IConnection connection : allConnections) {
				if (connection instanceof IOpenShiftConnection) {
					connections.put((IOpenShiftConnection) connection,
							new ConnectionWrapper(OpenshiftUIModel.this, (IOpenShiftConnection) connection));
				}
			}
		}
		registry.addListener(listener);
	}

	protected void addConnection(IConnection connection) {
		synchronized (connections) {
			if (connections.containsKey(connection)) {
				return;
			}
			connections.put((IOpenShiftConnection) connection,
					new ConnectionWrapper(OpenshiftUIModel.this, (IOpenShiftConnection) connection));
		}
	}

	protected void removeConnection(IConnection connection) {
		synchronized (connections) {
			connections.remove(connection);
		}
	}

	@Override
	public OpenshiftUIModel getRoot() {
		return this;
	}

	public Collection<ConnectionWrapper> getConnections() {
		synchronized (connections) {
			return new ArrayList<ConnectionWrapper>(connections.values());
		}
	}

	public ConnectionWrapper getConnectionWrapperForConnection(IConnection connection) {
		synchronized (connections) {
			return connections.get(connection);
		}
	}

	@Override
	public void refresh() {
		Map<IOpenShiftConnection, ConnectionWrapper> updated = new HashMap<>();
		boolean changed = false;
		synchronized (connections) {
			HashMap<IOpenShiftConnection, ConnectionWrapper> oldWrappers = new HashMap<>(connections);
			connections.clear();
			for (IOpenShiftConnection connection : ConnectionsRegistrySingleton.getInstance()
					.getAll(IOpenShiftConnection.class)) {
				ConnectionWrapper existingWrapper = oldWrappers.remove(connection);

				if (existingWrapper == null) {
					ConnectionWrapper newWrapper = new ConnectionWrapper(this, connection);
					connections.put(connection, newWrapper);
					changed = true;
				} else {
					connections.put(connection, existingWrapper);
					updated.put(connection, existingWrapper);
				}
			}
			if (!oldWrappers.isEmpty()) {
				changed = true;
			}
		}

		if (changed) {
			fireChanged(this);
		}

		updated.keySet().forEach(r -> {
			ConnectionWrapper wrapper = updated.get(r);
			wrapper.updateWith(r);
		});
		for (ConnectionWrapper connection : getConnections()) {
			connection.refresh();
		}
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return super.equals(o);
	}
}
