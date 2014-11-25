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

import java.util.List;

import org.jboss.tools.openshift.core.ConnectionType;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;

/**
 * @author adietish
 */
public class ConnectionsModelFake extends ConnectionsModel {

	private ConnectionsChange change = new ConnectionsChange();
	private List<String> savedDefaultHosts;
	private List<String> savedCustomHosts;
	
	public ConnectionsModelFake() {
		change.listenTo(this);
	}

	@Override
	protected String[] loadPersistedDefaultHosts() {
		return new String[]{};
	}

	@Override
	protected String[] loadPersistedCustomHosts() {
		return new String[]{};
	}

	@Override
	protected void saveDefaultHostConnections(List<String> usernames) {
		this.savedDefaultHosts = usernames;
	}

	@Override
	protected void saveCustomHostConnections(List<String> connectionUrls) {
		this.savedCustomHosts = connectionUrls;
	}

	public ConnectionsChange getChange() {
		return change;
	}
	
	public List<String> getSavedDefaultHosts() {
		return savedDefaultHosts;
	}

	public List<String> getSavedCustomHosts() {
		return savedCustomHosts;
	}

	public static class ConnectionsChange {

		private Connection notifiedConnection;

		private boolean additionNotified;
		private boolean removalNotified;
		private boolean changeNotified;

		public void listenTo(ConnectionsModel model) {
			model.addListener(new Listener());
		}

		public boolean isAdditionNotified() {
			return additionNotified;
		}

		public boolean isRemovalNotified() {
			return removalNotified;
		}

		public boolean isChangeNotified() {
			return changeNotified;
		}

		public Connection getConnection() {
			return notifiedConnection;
		}

		private class Listener implements IConnectionsModelListener {

			@Override
			public void connectionAdded(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				additionNotified = true;
				notifiedConnection = (Connection) connection;
			}

			@Override
			public void connectionRemoved(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				removalNotified = true;
				notifiedConnection = (Connection)connection;
			}

			@Override
			public void connectionChanged(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				changeNotified = true;
				notifiedConnection = (Connection)connection;
			}
		}
	}
}
