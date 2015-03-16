package org.jboss.tools.openshift.test.core.connection;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;

public class ConnectionsChange {

	private IConnection notifiedConnection;

	private boolean additionNotified;
	private boolean removalNotified;
	private boolean changeNotified;

	public ConnectionsChange(ConnectionsRegistry registry) {
		registry.addListener(new Listener());
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

	public IConnection getConnection() {
		return notifiedConnection;
	}
	
	public void reset() {
		this.additionNotified = false;
		this.removalNotified = false;
		this.changeNotified = false;
		this.notifiedConnection = null;
	}

	private class Listener implements IConnectionsRegistryListener {

		@Override
		public void connectionAdded(IConnection connection) {
			additionNotified = true;
			notifiedConnection = connection;
		}

		@Override
		public void connectionRemoved(IConnection connection) {
			removalNotified = true;
			notifiedConnection = connection;
		}

		@Override
		public void connectionChanged(IConnection connection) {
			changeNotified = true;
			notifiedConnection = connection;
		}
	}
}