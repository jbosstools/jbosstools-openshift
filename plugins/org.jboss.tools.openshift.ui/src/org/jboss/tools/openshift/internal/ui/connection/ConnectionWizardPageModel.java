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
package org.jboss.tools.openshift.internal.ui.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_CONNECTION = "selectedConnection";
	public static final String PROPERTY_USERNAME = "username";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";
	public static final String PROPERTY_USE_DEFAULTSERVER = "useDefaultServer";
	public static final String PROPERTY_VALID = "valid";
	public static final String PROPERTY_CREATE_CONNECTION = "createConnection";

	final private IConnectionAwareModel wizardModel;
	private IConnection selectedConnection;
	final private List<String> servers;
	private boolean isDefaultServer = true;
	private IStatus valid;
	private String host;
	private String username;
	private String password;
	private boolean isRememberPassword;
	private IConnection newConnection;
	private boolean allowConnectionChange;

	ConnectionWizardPageModel(IConnectionAwareModel wizardModel, boolean allowConnectionChange) {
		this.wizardModel = wizardModel;
		this.allowConnectionChange = allowConnectionChange;
		this.selectedConnection = null2NewConnectionMarker(wizardModel.getConnection());
		this.servers = getServers(selectedConnection);
		updateFrom(selectedConnection);
	}

	private void updateFrom(IConnection connection) {
//		if (isCreateNewConnection(connection)) {
//			setUsername(getDefaultUsername());
//			setUseDefaultServer(true);
//			setDefaultHost();
//			setPassword(null);
//		} else {
//			setUsername(connection.getUsername());
//			setHost(connection.getHost());
//			setUseDefaultServer(connection.isDefaultHost());
//			setRememberPassword(connection.isRememberPassword());
//			setPassword(connection.getPassword());
//		}
	}

	private boolean isCreateNewConnection(IConnection connection) {
		return connection instanceof NewConnectionMarker;
	}

	protected String getDefaultUsername() {
//		String username = ExpressPreferences.INSTANCE.getLastUsername();
//		if (StringUtils.isEmpty(username)) {
//			try {
//				username = new OpenShiftConfiguration().getRhlogin();
//			} catch (IOException e) {
//				OpenShiftCommonUIActivator.log("Could not load default user name from OpenShift configuration.", e);
//			} catch (OpenShiftException e) {
//				OpenShiftCommonUIActivator.log("Could not load default user name from OpenShift configuration.", e);
//			}
//		}
//		return username;
		return null;
	}

	private IConnection null2NewConnectionMarker(IConnection connection) {
		if (connection == null) {
			return new NewConnectionMarker();
		} else {
			return connection;
		}
	}

	public void setSelectedConnection(IConnection connection) {
		if (Diffs.equals(selectedConnection, connection)) {
			return;
		}
		updateFrom(connection);
		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = connection);
	}

	public IConnection getSelectedConnection() {
		return selectedConnection;
	}

	public Collection<IConnection> getConnections() {
		if (allowConnectionChange) {
			Collection<IConnection> connections = ConnectionsRegistrySingleton.getInstance().getAll();
			return connections;
		} else {
			return Collections.singletonList(selectedConnection);
		}
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
	
	private List<String> getServers(IConnection connection) {
		List<String> servers = new ArrayList<String>();
		HashSet<String> uniqueServers = new HashSet<String>();
		uniqueServers.add(getDefaultServer());
		servers.add(connection.getHost());
		return servers;
	}

	private String getDefaultServer() {
		return null;
//		try {
//			return new OpenShiftConfiguration().getLibraServer();
//		} catch (Exception e) {
//			OpenShiftCommonUIActivator.log(e);
//			return null;
//		}
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
//		setHost(ExpressConnectionUtils.getDefaultHostUrl());
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
//		try {
//			if((status = tryConnection(true)) != Status.OK_STATUS){
//				status = tryConnection(false);
//			}
//		} catch (NotFoundOpenShiftException e) {
//			// valid user without domain
//		} catch (Exception e) {
//			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
//					NLS.bind("Unknown error, can not verify user {0} - see Error Log for details", username));
//			OpenShiftCommonUIActivator.log(e);
//		}
//		setValid(status);
		return status;
	}
	
	private IStatus tryConnection(boolean legacy){
//		try {
//			IConnection connection = null;
//			if (isCreateNewConnection()
//					|| isSelectedConnectionChanged()) {
//				connection = createConnection(legacy);
//			} else {
//				connection = selectedConnection;
//				connection.accept(new IConnectionVisitor() {
//					@Override
//					public void visit(KubernetesConnection connection) {
//					}
//					
//					@Override
//					public void visit(ExpressConnection connection) {
//						connection.setRememberPassword(isRememberPassword());
//					}
//				});
//			}
//			connection.connect();
//			this.newConnection = connection;
//		} catch (OpenShiftTimeoutException e) {
//			return StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, 
//					NLS.bind("Could not reach host at {0}. ExpressConnection timeouted.", host));
//		} catch (InvalidCredentialsOpenShiftException e) {
//			return StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, 
//					NLS.bind("The credentials for user {0} are not valid", username));
//		} catch (OpenShiftException e) {
//			OpenShiftCommonUIActivator.log(e);
//			return StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, 
//					NLS.bind("Unknown error, can not verify user {0} - see Error Log for details", username));
//		}
		return Status.OK_STATUS;
	}

	private IConnection createConnection(boolean legacy) {
		return null;
//		String host = this.host;
//		if(!legacy){
//			try {
//				return new KubernetesConnection(host, username, password);
//			} catch (MalformedURLException e) {
//				throw new RuntimeException(e);
//			}
//		} else if (isDefaultServer) {
//			return new ExpressConnection(username, password, isRememberPassword, OpenshiftCoreUIIntegration.getDefault().getSSLCertificateCallback());
//		} else {
//			return new ExpressConnection(username, password, host, isRememberPassword, OpenshiftCoreUIIntegration.getDefault().getSSLCertificateCallback());
//		}
	}
	
	private boolean isSelectedConnectionChanged() {
		return false;
//		return !password.equals(selectedConnection.getPassword());
	}

	public IConnection getConnection() {
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
			newConnection.accept(new IConnectionVisitor(){
				@Override
				public void visit(ExpressConnection connection) {
					ConnectionsRegistrySingleton.getInstance().add(connection);
				}

				@Override
				public void visit(KubernetesConnection connection) {
					ConnectionsRegistrySingleton.getInstance().add(connection);
				}
				
			});
		} else {
			if (selectedConnection != newConnection) {
				selectedConnection.accept(new IConnectionVisitor() {
					@Override
					public void visit(KubernetesConnection connection) {
					}
					
					@Override
					public void visit(ExpressConnection selectedConnection) {
						if(!(newConnection instanceof ExpressConnection))
							return;
						// dont update since we were editing the connection we we already holding
						// JBIDE-14771
						selectedConnection.editConnection((ExpressConnection) newConnection);
					}
				});
			}
			// we may have get started from new wizard without a iConnection
			// in wizard model: set it to wizard model
			wizardModel.setConnection(selectedConnection);
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(selectedConnection);
		}
	}

	public void saveRecentConnection() {
//		IConnection connection = getConnection();
//		if (connection != null) {
//			connection.accept(new IConnectionVisitor() {
//				
//				@Override
//				public void visit(KubernetesConnection connection) {
//					//TODO figure out how to save connection
//				}
//				
//				@Override
//				public void visit(ExpressConnection connection) {
//					ConnectionsRegistrySingleton.getInstance().setRecent(connection);
//				}
//			});
//		}
	}
}
