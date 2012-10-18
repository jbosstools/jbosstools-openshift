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
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.CollectionUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.NewConnectionMarker;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

import com.openshift.client.IUser;
import com.openshift.client.InvalidCredentialsOpenShiftException;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;
import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_CONNECTION = "selectedConnection";
	public static final String PROPERTY_USERNAME = "username";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_SERVER = "server";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";
	public static final String PROPERTY_USE_DEFAULTSERVER = "useDefaultServer";
	public static final String PROPERTY_VALID = "valid";
	public static final String PROPERTY_DUPLICATE_CONNECTION = "duplicateConnection";
	public static final String PROPERTY_CREATE_CONNECTION = "createConnection";
	
	final private IConnectionAwareModel wizardModel;
	private Connection selectedConnection;
	final private List<String> servers;
	private boolean isDefaultServer = true;
	private Connection editedConnection;
	private IStatus valid;
	private boolean isCreateNewConnection;
	
	public ConnectionWizardPageModel(IConnectionAwareModel wizardModel) {
		this.wizardModel = wizardModel;
		Connection wizardModelConnection = wizardModel.getConnection();
		this.selectedConnection = getSelectedConnection(wizardModelConnection);
		this.isCreateNewConnection = getIsCreateNewConnection(selectedConnection);
		this.editedConnection = createConnection(selectedConnection);
		this.servers = getServers(editedConnection);
	}

	private boolean getIsCreateNewConnection(Connection connection) {
		return connection instanceof NewConnectionMarker;
	}

	private Connection getSelectedConnection(Connection wizardModelConnection) {
		if (wizardModelConnection == null) {
			Connection recentConnection = ConnectionsModel.getDefault().getRecentConnection();
			if (recentConnection != null) {
				return recentConnection;
			} else {
				return new NewConnectionMarker();
			}
		} else {
			return wizardModelConnection;
		}
	}

	/**
	 * Returns a new Connection for a given connection. The new connection gets
	 * created with the username and password from the preferences (and secure
	 * storage) if <code>null</code>.
	 * <p>
	 * We always have to create a new connection since you can cancel the wizard
	 * and dont want changes to your existing connection then.
	 * 
	 * @param connection
	 * @return
	 * 
	 * @see Connection
	 * @see IUser
	 */
	private Connection createConnection(Connection connection) {
		Connection newConnection = null;
		if (connection instanceof NewConnectionMarker
				|| connection == null) {
			newConnection = new Connection();
		} else {
			newConnection = new Connection(connection);
		}
		return newConnection;
	}

	public void setSelectedConnection(Connection connection) {
		if ((this.selectedConnection instanceof NewConnectionMarker 
				&& connection instanceof NewConnectionMarker)
				|| Diffs.equals(selectedConnection, connection)) {
			return;
		}
		this.isCreateNewConnection = getIsCreateNewConnection(connection);
		setEditedConnection(createConnection(connection));
		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = connection);
	}
	
	private void setEditedConnection(Connection connection) {
		Connection oldValue = editedConnection;
		this.editedConnection = connection;
		resetValid();
		firePropertyChange(PROPERTY_SERVER, oldValue.getHost(), this.editedConnection.getHost());
		firePropertyChange(PROPERTY_USERNAME, oldValue.getUsername(), this.editedConnection.getUsername());
		firePropertyChange(PROPERTY_PASSWORD, oldValue.getPassword(), this.editedConnection.getPassword());
	}

	public Connection getSelectedConnection() {
		return selectedConnection;
	}

	public List<Connection> getConnections() {
		List<Connection> connections = CollectionUtils.toList(ConnectionsModel.getDefault().getConnections());
		connections.add(new NewConnectionMarker());
		return connections;
	}

	public boolean isUseDefaultServer() {
		return isDefaultServer;
	}

	public void setUseDefaultServer(boolean isDefaultServer) {
		if (this.isDefaultServer != isDefaultServer) {
			firePropertyChange(PROPERTY_USE_DEFAULTSERVER,
					this.isDefaultServer, this.isDefaultServer = isDefaultServer);
			resetValid();
			if (isDefaultServer) {
				setServer(new Connection().getHost());
			}
		}
	}

	private List<String> getServers(Connection connection) {
		List<String> servers = new ArrayList<String>();
		HashSet<String> uniqueServers = new HashSet<String>();
		uniqueServers.add(getDefaultServer());
		servers.add(connection.getHost());
		return servers;
	}

	private String getDefaultServer() {
		try {
			return new OpenShiftConfiguration().getLibraServer();
		} catch (Exception e) {
			OpenShiftUIActivator.log(e);
			return null;
		}
	}

	public String getUsername() {
		return editedConnection.getUsername();
	}

	public void setUsername(String username) {
		if (!Diffs.equals(editedConnection.getUsername(), username)) {
			firePropertyChange(PROPERTY_USERNAME, editedConnection.getUsername(),
					editedConnection.setUsername(username));
			resetValid();
			fireDuplicateConnectionUpdated();
		}
	}

	public String getPassword() {
		return editedConnection.getPassword();
	}

	public void setPassword(String password) {
		if (!Diffs.equals(password, editedConnection.getPassword())) {
			firePropertyChange(PROPERTY_PASSWORD, editedConnection.getPassword(),
					editedConnection.setPassword(password));
			resetValid();
		}
	}

	public String getServer() {
		return editedConnection.getHost();
	}

	public void setServer(String server) {
		if (server == null) { // workaround
			return;
		}
		if (!Diffs.equals(editedConnection.getHost(), server)) {
			firePropertyChange(PROPERTY_SERVER, editedConnection.getHost(), editedConnection.setHost(server));
			resetValid();
			fireDuplicateConnectionUpdated();
		}
	}

	public List<String> getServers() {
		return servers;
	}

	public boolean isRememberPassword() {
		return editedConnection.isRememberPassword();
	}

	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD,
				editedConnection.isRememberPassword(), editedConnection.setRememberPassword(rememberPassword));
	}

	private void resetValid() {
		setValid(null);
	}

	private void setValid(IStatus status) {
		firePropertyChange(PROPERTY_VALID, this.valid, this.valid = status);
	}

	public IStatus getValid() {
		return valid;
	}

	public IStatus connect() {
		IStatus status = Status.OK_STATUS;
		try {
			if (!editedConnection.isConnected()) {
				try {
					editedConnection.connect();
				} catch (OpenShiftTimeoutException e) {
					status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not reach server at {0}. Connection timeouted.", editedConnection.getHost()));
				} catch (InvalidCredentialsOpenShiftException e) {
					status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"The credentials for user {0} are not valid", editedConnection.getUsername()));
				} catch (OpenShiftException e) {
					status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not check user credentials: {0}", e.getMessage()));
				}
			}
		} catch (NotFoundOpenShiftException e) {
			// valid user without domain
		} catch (Exception e) {
			status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
					"Could not check user credentials: {0}.", e.getMessage()));
		}
		setValid(status);
		return status;
	}

	public boolean isDuplicateConnection() {
		return editedConnection == null
				|| ConnectionsModel.getDefault().hasConnection(editedConnection);
	}

	private void fireDuplicateConnectionUpdated() {
		firePropertyChange(PROPERTY_DUPLICATE_CONNECTION, null, isDuplicateConnection());
	}

	public Connection getConnection() {
		return editedConnection;
	}

	public boolean isCreateNewConnection() {
		return isCreateNewConnection;
	}

	/**
	 * Updates the connection that this wizard operates on or creates a new one.
	 * Will create a new connection if the wizard had no connection to operate
	 * on or if there was one and it was told to create a new one by the given
	 * flag.
	 * 
	 * @param create
	 *            if true, creates a new connection if the wizard had a
	 *            connection to edit. Updates the existing one otherwise.
	 */
	public void createOrUpdateConnection() {
		Connection wizardModelConnection = wizardModel.getConnection();
		if (wizardModelConnection == null
				|| isCreateNewConnection()) {
			wizardModel.setConnection(editedConnection);
			ConnectionsModel.getDefault().addConnection(editedConnection);
			// editedConnection.save();
		} else {
			wizardModelConnection.update(editedConnection);
			ConnectionsModel.getDefault().fireConnectionChanged(wizardModelConnection);
			// wizardModelConnection.save();
		}
	}

}
