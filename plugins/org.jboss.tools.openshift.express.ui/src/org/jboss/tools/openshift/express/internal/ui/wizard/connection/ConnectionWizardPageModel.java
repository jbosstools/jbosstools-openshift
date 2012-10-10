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
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

import com.openshift.client.IUser;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;
import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_USERNAME = "username";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_SERVER = "server";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";
	public static final String PROPERTY_USE_DEFAULTSERVER = "useDefaultServer";
	public static final String PROPERTY_VALID = "valid";

	final private List<String> servers;
	private boolean isDefaultServer = true;
	private IConnectionAwareModel wizardModel;
	private Connection connection;
	private IStatus valid;

	public ConnectionWizardPageModel(IConnectionAwareModel wizardModel) {
		this.wizardModel = wizardModel;
		this.connection = createConnection(wizardModel.getConnection());
		this.servers = getServers(connection);
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
		Connection newUser = null;
		if (connection != null) {
			newUser = new Connection(connection);
		} else {
			newUser = new Connection();
		}
		return newUser;
	}

	public boolean isUseDefaultServer() {
		return isDefaultServer;
	}

	public void setUseDefaultServer(boolean isDefaultServer) {
		if (this.isDefaultServer != isDefaultServer) {
			firePropertyChange(PROPERTY_USE_DEFAULTSERVER,
					this.isDefaultServer, this.isDefaultServer = isDefaultServer);
			if (isDefaultServer) {
				setServer(connection.getHost());
			}
			resetValid();
		}
	}

	private List<String> getServers(Connection user) {
		List<String> servers = new ArrayList<String>();
		HashSet<String> uniqueServers = new HashSet<String>();
		uniqueServers.add(getDefaultServer());
		servers.add(user.getHost());
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
		return connection.getUsername();
	}

	public void setUsername(String username) {
		if (!Diffs.equals(connection.getUsername(), username)) {
			firePropertyChange(PROPERTY_USERNAME, connection.getUsername(), connection.setUsername(username));
			resetValid();
		}
	}

	public String getPassword() {
		return connection.getPassword();
	}

	public void setPassword(String password) {
		if (!Diffs.equals(password, connection.getPassword())) {
			firePropertyChange(PROPERTY_PASSWORD, connection.getPassword(), connection.setPassword(password));
			resetValid();
		}
	}

	public String getServer() {
		return connection.getHost();
	}

	public void setServer(String server) {
		if (server == null) { // workaround
			return;
		}
		if (!Diffs.equals(connection.getHost(), server)) {
			firePropertyChange(PROPERTY_SERVER, connection.getHost(), connection.setHost(server));
			resetValid();
		}
	}

	public List<String> getServers() {
		return servers;
	}

	public boolean isRememberPassword() {
		return connection.isRememberPassword();
	}

	public void setRememberPassword(boolean rememberPassword) {
		firePropertyChange(PROPERTY_REMEMBER_PASSWORD,
				connection.isRememberPassword(), connection.setRememberPassword(rememberPassword));
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
			if (!connection.isConnected()) {
				try {
					connection.connect();
				} catch (OpenShiftTimeoutException e) {
					status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not reach server at {0}. Connection timeouted.", connection.getHost()));
				} catch (OpenShiftException e) {
					status = OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"The credentials for user {0} are not valid", connection.getUsername()));
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

	/**
	 * Returns <code>true</code> if the connection that this wizards edits was
	 * changed in a way that would suggest creating a new connection (instead of
	 * updating it). The current implementation would suggests to create a new
	 * connection as soon as the username or the server was changed.
	 * 
	 * @return true if you should create a new connection
	 */
	public boolean shouldCreateNewConnection() {
		Connection wizardModelConnection = wizardModel.getConnection();
		return wizardModelConnection != null
				&& (
				// username changed
				!wizardModelConnection.getUsername().equals(connection.getUsername())
				// server changed
				|| !wizardModelConnection.getHost().equals(connection.getHost()));
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
	public void createOrUpdateConnection(boolean create) {
		Connection wizardModelConnection = wizardModel.getConnection();
		if (wizardModelConnection == null
				|| create) {
			wizardModel.setConnection(connection);
			ConnectionsModel.getDefault().addConnection(connection);
//			connection.save();
		} else {
			wizardModelConnection.update(connection);
			ConnectionsModel.getDefault().fireConnectionChanged(wizardModelConnection);
//			wizardModelConnection.save();
		}
	}

	public Connection getConnection() {
		return connection;
	}
}
