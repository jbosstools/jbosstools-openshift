/*******************************************************************************
 * Copyright (c) 2012-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.AutomaticConnectionFactoryMarker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTION_FACTORY = "connectionFactory";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_CONNECTED = "connected";
	public static final String PROPERTY_USE_DEFAULT_HOST = "useDefaultHost";

	final private IConnectionAwareModel wizardModel;
	private IConnection connection;
	private IStatus connected;
	private String host;
	private boolean allowConnectionChange;
	private final ConnectionsFactoryTracker connectionsFactory;
	private IConnectionFactory connectionFactory;
	private boolean useDefaultHost;
	
	ConnectionWizardPageModel(IConnectionAwareModel wizardModel, boolean allowConnectionChange) {
		this.wizardModel = wizardModel;
		this.allowConnectionChange = allowConnectionChange;
		this.connection = wizardModel.getConnection();
		this.connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		this.connectionFactory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_EXPRESS_ID);
		setUseDefaultHost(true);
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

	protected String getDefaultUsername() {
		return "";
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
	}

	public void resetConnection() {
		setConnection(null);
	}

	public void setConnection(IConnection connection) {
//		if (Diffs.equals(this.connection, connection)) {
//			return;
//		}
//		updateFrom(connection);
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}

	public IConnection getConnection() {
		return connection;
	}

	public void setConnectionFactory(IConnectionFactory factory) {
		firePropertyChange(PROPERTY_CONNECTION_FACTORY, this.connectionFactory, this.connectionFactory = factory);
		if (factory != null) {
			setUseDefaultHost(connectionFactory.hasDefaultHost());
			setConnection(null);
		}
	}

	public IConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
	
	public Collection<IConnection> getConnections() {
		if (allowConnectionChange) {
			List<IConnection> connections = new ArrayList<IConnection>();
			connections.add(NewConnectionMarker.getInstance());
			connections.addAll(ConnectionsRegistrySingleton.getInstance().getAll());
			return connections;
		} else {
			return Collections.singletonList(connection);
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
		resetConnected();
	}
	
	/**
	 * Creates a connection for the host present in this model and stores it in this model. If
	 * the given host is empty or <code>null</code> no connection is created.
	 * Does blocking remote http calls.
	 * 
	 * @param host
	 *            the host to get the connection for.
	 */
	public void createConnection() {
		createConnection(host);
	}

	/**
	 * Creates a connection for the given host and stores it in this model. If
	 * the given host is empty or <code>null</code> no connection is created.
	 * Does blocking remote http calls.
	 * 
	 * @param host
	 *            the host to get the connection for.
	 */
	private void createConnection(String host) {
		IConnection connection = null;
		if (connectionFactory != null
				&& !StringUtils.isEmpty(host)) {
			if (connectionFactory instanceof AutomaticConnectionFactoryMarker) {
				try {
					connection = connectionsFactory.create(host);
					if (connection == null) {
						setConnected(StatusFactory.errorStatus(
								OpenShiftCommonUIActivator.PLUGIN_ID,
								NLS.bind("The host at {0} is no OpenShift host", host)));
					} else {
						// switches <automatic> to detected type
						setConnectionFactory(connectionsFactory.getByConnection(connection.getClass()));
					}
				} catch (IOException e) {
					resetConnection();
					setConnected(StatusFactory.errorStatus(
							OpenShiftCommonUIActivator.PLUGIN_ID, NLS.bind("Could not connect to host at {0}.", host),
							e));
				}
			} else {
				try {
					connection = connectionFactory.create(host);
					if (!connection.canConnect()) {
						connection = null;
					}
				} catch (IOException e) {
					resetConnection();
					setConnected(StatusFactory.errorStatus(
							OpenShiftCommonUIActivator.PLUGIN_ID, NLS.bind("Could not connect to host at {0}.", host),
							e));
				}
			}
		}

			setConnection(connection);
	}
	
	public void setUseDefaultHost(boolean useDefaultHost) {
		firePropertyChange(PROPERTY_USE_DEFAULT_HOST, this.useDefaultHost, this.useDefaultHost = useDefaultHost);
		if (useDefaultHost) {
			setHost(connectionFactory.getDefaultHost());
		}
		resetConnected();
	}
	
	public boolean isUseDefaultHost() {
		return useDefaultHost;
	}

	private void resetConnected() {
		setConnected(null);
	}

	private void setConnected(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTED, this.connected, this.connected = status);
	}

	public IStatus getConnected() {
		return connected;
	}

	public IStatus connect() {
		IStatus status = Status.OK_STATUS;
		try {
			if(connection != null) {
				connection.connect();
			}
//		} catch (NotFoundOpenShiftException e) {
//			// connected user without domain
		} catch (Exception e) {
			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
					NLS.bind("Unknown error, can not verify connection to host {0} - see Error Log for details", connection.getHost()));
			OpenShiftCommonUIActivator.log(e);
		}
		setConnected(status);
		return status;
	}
	
	private IStatus tryConnection(boolean legacy){
//		try {
//			IConnection connection = null;
//			if (isCreateNewConnection()
//					|| isSelectedConnectionChanged()) {
//				connection = createConnection(legacy);
//			} else {
//				connection = connection;
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
//					NLS.bind("The credentials for user {0} are not connected", username));
//		} catch (OpenShiftException e) {
//			OpenShiftCommonUIActivator.log(e);
//			return StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, 
//					NLS.bind("Unknown error, can not verify user {0} - see Error Log for details", username));
//		}
		return Status.OK_STATUS;
	}
	
	private boolean isSelectedConnectionChanged() {
		return false;
//		return !password.equals(connection.getPassword());
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
	
	public Collection<IConnectionFactory> getConnectionFactories() {
		List<IConnectionFactory> connectionFactories = new ArrayList<IConnectionFactory>();
		connectionFactories.add(new AutomaticConnectionFactoryMarker());
		connectionFactories.addAll(connectionsFactory.getAll());
		return connectionFactories;
	}
	
	public void dispose() {
		connectionsFactory.close();
	}
}
