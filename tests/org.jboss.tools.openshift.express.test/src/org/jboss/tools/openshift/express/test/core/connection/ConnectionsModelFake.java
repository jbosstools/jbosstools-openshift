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
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.internal.core.connection.IConnectionsModelListener;

/**
 * @author adietish
 */
public class ConnectionsModelFake extends ConnectionsModel {

	ConnectionsChange change = new ConnectionsChange();
	
	public ConnectionsModelFake() {
		change.listenTo(this);
	}

	@Override
	protected void load() {
		// dont load anything
	}

	@Override
	public void save() {
		// dont save anything
	}

	public ConnectionsChange getChange() {
		return change;
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
			public void connectionAdded(Connection connection) {
				additionNotified = true;
				notifiedConnection = connection;
			}

			@Override
			public void connectionRemoved(Connection connection) {
				removalNotified = true;
				notifiedConnection = connection;
			}

			@Override
			public void connectionChanged(Connection connection) {
				changeNotified = true;
				notifiedConnection = connection;
			}
		}
	}
}
