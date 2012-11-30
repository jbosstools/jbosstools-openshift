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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.CollectionUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.NewConnectionMarker;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

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
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";
	public static final String PROPERTY_USE_DEFAULTSERVER = "useDefaultServer";
	public static final String PROPERTY_VALID = "valid";
	public static final String PROPERTY_CREATE_CONNECTION = "createConnection";

	final private IConnectionAwareModel wizardModel;
	private Connection selectedConnection;
	final private List<String> servers;
	private boolean isDefaultServer = true;
	private IStatus valid;
	private String host;
	private String username;
	private String password;
	private boolean isRememberPassword;
	private Connection newConnection;

	public ConnectionWizardPageModel(IConnectionAwareModel wizardModel) {
		this.wizardModel = wizardModel;
		Connection wizardModelConnection = wizardModel.getConnection();
		this.selectedConnection = getSelectedConnection(wizardModelConnection);
		this.servers = getServers(selectedConnection);
		updateFrom(selectedConnection);
	}

	private void updateFrom(Connection connection) {
		if (isCreateNewConnection(connection)) {
			setUsername(getDefaultUsername());
			setDefaultHost();
			setPassword(null);
		} else {
			setUsername(connection.getUsername());
			setHost(connection.getHost());
			setUseDefaultServer(connection.isDefaultHost());
			setRememberPassword(connection.isRememberPassword());
			if (connection.isRememberPassword()) {
				setPassword(connection.getPassword());
			} else {
				setPassword(null);
			}
		}
	}

	private boolean isCreateNewConnection(Connection connection) {
		return connection instanceof NewConnectionMarker;
	}

	protected String getDefaultUsername() {
		String username = OpenShiftPreferences.INSTANCE.getLastUsername();
		if (StringUtils.isEmpty(username)) {
			try {
				username = new OpenShiftConfiguration().getRhlogin();
			} catch (IOException e) {
				Logger.error("Could not load default user name from OpenShift configuration.", e);
			} catch (OpenShiftException e) {
				Logger.error("Could not load default user name from OpenShift configuration.", e);
			}
		}
		return username;
	}

	private Connection getSelectedConnection(Connection wizardModelConnection) {
		if (wizardModelConnection == null) {
			Connection recentConnection = ConnectionsModelSingleton.getInstance().getRecentConnection();
			if (recentConnection != null) {
				return recentConnection;
			} else {
				return new NewConnectionMarker();
			}
		} else {
			return wizardModelConnection;
		}
	}

	public void setSelectedConnection(Connection connection) {
		if (Diffs.equals(selectedConnection, connection)) {
			return;
		}
		updateFrom(connection);
		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = connection);
	}

	public Connection getSelectedConnection() {
		return selectedConnection;
	}

	public List<Connection> getConnections() {
		List<Connection> connections = CollectionUtils.toList(ConnectionsModelSingleton.getInstance().getConnections());
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
			if (isDefaultServer) {
				setDefaultHost();
			}
			resetValid();
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
		return username;
	}

	public void setUsername(String username) {
		if (!Diffs.equals(this.username, username)) {
			firePropertyChange(PROPERTY_USERNAME, this.username, this.username = username);
			resetValid();
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (!Diffs.equals(password, this.password)) {
			firePropertyChange(PROPERTY_PASSWORD, this.password, this.password = password);
			resetValid();
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if (!Diffs.equals(this.host, host)) {
			firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
			resetValid();
		}
	}

	private void setDefaultHost() {
		setHost(ConnectionUtils.getDefaultHostUrl());
	}
	
	public List<String> getServers() {
		return servers;
	}

	public boolean isRememberPassword() {
		return isRememberPassword;
	}

	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD,
				this.isRememberPassword, this.isRememberPassword = rememberPassword);
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
			try {
				Connection connection = null;
				if (isCreateNewConnection()
						|| isSelectedConnectionChanged()) {
					connection = createConnection();
				} else {
					connection = selectedConnection;
					connection.setRememberPassword(isRememberPassword());
				}
				connection.connect();
				this.newConnection = connection;
			} catch (OpenShiftTimeoutException e) {
				status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
						"Could not reach host at {0}. Connection timeouted.", host));
			} catch (InvalidCredentialsOpenShiftException e) {
				status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
						"The credentials for user {0} are not valid", username));
			} catch (OpenShiftException e) {
				status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
						"The credentials for user {0} are not valid", username));
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

	private Connection createConnection() {
		String host = this.host; 
		if (isDefaultServer) {
			return new Connection(username, password, isRememberPassword);
		} else {
			return new Connection(username, password, host, isRememberPassword);
		}
	}
	
	private boolean isSelectedConnectionChanged() {
		return !password.equals(selectedConnection.getPassword());
	}

	public Connection getConnection() {
		return newConnection;
	}

	public boolean isCreateNewConnection() {
		return isCreateNewConnection(selectedConnection);
	}

	/**
	 * Updates the newConnection that this wizard operates on or creates a new one.
	 * Will create a new newConnection if the wizard had no newConnection to operate
	 * on or if there was one and it was told to create a new one by the given
	 * flag.
	 * 
	 * @param create
	 *            if true, creates a new newConnection if the wizard had a
	 *            newConnection to edit. Updates the existing one otherwise.
	 */
	public void createOrUpdateConnection() {
		if (isCreateNewConnection()) {
			wizardModel.setConnection(newConnection);
			ConnectionsModelSingleton.getInstance().addConnection(newConnection);
			// editedConnection.save();
		} else {
			selectedConnection.update(newConnection);
			// we may have get started from new wizard without a connection in
			// wizard model: set it to wizard model
			wizardModel.setConnection(selectedConnection);
			ConnectionsModelSingleton.getInstance().fireConnectionChanged(selectedConnection);
			// wizardModelConnection.save();
		}
	}
}
