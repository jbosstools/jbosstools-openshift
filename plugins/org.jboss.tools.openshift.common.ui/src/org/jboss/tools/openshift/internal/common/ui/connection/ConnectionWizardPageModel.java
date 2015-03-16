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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.validation.ValidationStatus;
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
import org.jboss.tools.openshift.internal.common.ui.utils.ObservablePojoUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareModel;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_CONNECTION = "selectedConnection";
	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTION_FACTORY = "connectionFactory";
	public static final String PROPERTY_CONNECTION_FACTORY_ERROR = "connectionFactoryError";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_USE_DEFAULT_HOST = "useDefaultHost";
	public static final String PROPERTY_CONNECT_ERROR = "connectError";

	/** the connection that the user wants to edit */
	private IConnection selectedConnection;
	/** the connection that this wizard operates on */
	private IConnection connection;
	private IStatus connectionFactoryError;
	private String host;
	private boolean allowConnectionChange;
	private IConnectionFactory connectionFactory;
	private boolean useDefaultHost;
	private ConnectionsFactoryTracker connectionsFactory;
	private IConnectionAwareModel wizardModel;
	private IStatus connectError;
	private PropertyChangeListener connectionChangeListener = onConnectionChanged();
	
	ConnectionWizardPageModel(IConnectionAwareModel wizardModel, boolean allowConnectionChange) {
		this.wizardModel = wizardModel;
		this.allowConnectionChange = allowConnectionChange;
		this.connectionsFactory = createConnectionsFactory();
		init(wizardModel);
	}

	private ConnectionsFactoryTracker createConnectionsFactory() {
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		return connectionsFactory;
	}
	
	private void init(IConnectionAwareModel wizardModel) {
		this.connectionFactory = getDefaultConnectionFactory(connectionsFactory);
		this.connectError = Status.OK_STATUS;
		this.connectionFactoryError = Status.OK_STATUS;
		this.useDefaultHost = true;
		IConnection wizardConnection = wizardModel.getConnection();
		if (wizardConnection == null) {
			this.selectedConnection = NewConnectionMarker.getInstance();
			this.host = connectionFactory.getDefaultHost();
			this.connection = this.connectionFactory.create(host);
		} else {
			this.selectedConnection = wizardConnection;
			this.host = wizardConnection.getHost();
			this.connection = wizardConnection.clone();
			addConnectionListener(connection, this.connection, this.connectionChangeListener);
		}
	}

	private IConnectionFactory getDefaultConnectionFactory(IConnectionsFactory connectionsFactory) {
		return connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_EXPRESS_ID);
	}
	
	private void update(IConnection selectedConnection, IConnection connection, IConnectionFactory factory, boolean useDefaultHost, IStatus connectionFactoryError, IStatus connectError) {
		factory = updateFactory(selectedConnection, factory);
		connection = updateConnection(selectedConnection, connection, factory);
		String host = updateHost(connection, factory);
		useDefaultHost = updateUseDefaultHost(useDefaultHost, connection, factory);

		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = selectedConnection);
		firePropertyChange(PROPERTY_CONNECTION_FACTORY, this.connectionFactory, this.connectionFactory = factory);
		firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
		firePropertyChange(PROPERTY_USE_DEFAULT_HOST, this.useDefaultHost, this.useDefaultHost = useDefaultHost);
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		
		setConnectionFactoryError(connectionFactoryError);
		setConnectError(connectError);
	}

	private IConnectionFactory updateFactory(IConnection selectedConnection, IConnectionFactory factory) {
		if (selectedConnection != null
				&& !selectedConnection.equals(this.selectedConnection)) {
			// selectedConnection changed
			if (!(selectedConnection instanceof NewConnectionMarker)) {
				factory = connectionsFactory.getByConnection(selectedConnection.getClass());
			}
		}
		return factory;
	}

	private String updateHost(IConnection connection, IConnectionFactory factory) {
		String host = this.host;
		if (connection != null
				&& !connection.equals(this.connection)) {
			// connection changed
			host = connection.getHost();
		} else if (factory != null
				&& !factory.equals(connectionFactory)) {
			// factory changed
			if (useDefaultHost
					&& !StringUtils.isEmpty(factory.getDefaultHost())) {
				host = factory.getDefaultHost();
			}
		}

		return host;
	}

	private boolean updateUseDefaultHost(boolean useDefaultHost, IConnection connection, IConnectionFactory factory) {
		if (factory != null
				&& !factory.equals(connectionFactory)) {
			// connection factory changed
			if (useDefaultHost) {
				useDefaultHost = factory.hasDefaultHost();
			}
		} else if (connection != null
				&& !connection.equals(this.connection)) {
				// connection changed
				useDefaultHost = connection.isDefaultHost();
		};
		return useDefaultHost;
	}
	
	private IConnection updateConnection(IConnection selectedConnection, IConnection connection, IConnectionFactory factory) {
		if (!Diffs.equals(selectedConnection, this.selectedConnection)) {
			// selected connection changed
			if (selectedConnection instanceof NewConnectionMarker) {
				if (factory instanceof AutomaticConnectionFactoryMarker
						|| factory == null) {
					connection = null;
				} else {
					connection = factory.create(getHost());
				}
			} else {
				connection = selectedConnection.clone();
			}
		}
		
		if (!Diffs.equals(connection, this.connection)
				&& connection != null) {
			addConnectionListener(connection, this.connection, this.connectionChangeListener);
		}
		return connection;
	}
	
	private void addConnectionListener(IConnection newConnection, IConnection oldConnection, PropertyChangeListener listener) {
		ObservablePojoUtils.removePropertyChangeListener(listener, oldConnection);
		ObservablePojoUtils.addPropertyChangeListener(listener, newConnection);
	}

	private PropertyChangeListener onConnectionChanged() {
		return new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// reset connection error
				setConnectError(Status.OK_STATUS);
			}
		};
	}

	private boolean isNewConnection() {
		return selectedConnection instanceof NewConnectionMarker;
	}
	
	public void setSelectedConnection(IConnection selectedConnection) {
		update(selectedConnection, connection, connectionFactory, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}

	public IConnection getSelectedConnection() {
		return selectedConnection;
	}

	public void setConnection(IConnection connection) {
		setConnection(connection, true);
	}
	
	private void setConnection(IConnection connection, boolean updateModel) {
		update(selectedConnection, connection, connectionFactory, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}

	public IConnection getConnection() {
		return connection;
	}
	
	public void setConnectionFactory(IConnectionFactory factory) {
		update(NewConnectionMarker.getInstance(), null, factory, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
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
		update(selectedConnection, connection, connectionFactory, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}
	
	/**
	 * Creates a connection factory for the host present in this model and stores it in this model. If
	 * the given host is empty or <code>null</code> no connection is created.
	 * Does blocking remote http calls.
	 * 
	 * @param host
	 *            the host to get the connection for.
	 */
	public void createConnectionFactory() {
		createConnectionFactory(host, connectionsFactory);
	}

	/**
	 * Creates a connection factory for the given host and stores it in this model. If
	 * the given host is empty or <code>null</code> no connection is created.
	 * Does blocking remote http calls.
	 * 
	 * @param host
	 *            the host to get the connection for.
	 */
	private IConnectionFactory createConnectionFactory(String host, IConnectionsFactory connectionsFactory) {
		if (StringUtils.isEmpty(host)
				|| connectionsFactory == null) {
			return null;
		}
		
		IConnectionFactory factory = null;
		try {
			factory = connectionsFactory.getFactory(host);
			if (factory != null) {
				update(NewConnectionMarker.getInstance(),
						factory.create(host),
						factory,
						useDefaultHost,
						Status.OK_STATUS,
						Status.OK_STATUS);
			} else {
				update(selectedConnection,
						connection,
						connectionFactory,
						useDefaultHost,
						ValidationStatus.error(NLS.bind("The host at {0} is no OpenShift host", host)),
						Status.OK_STATUS);
			}
		} catch (IOException e) {
			update(selectedConnection,
					connection,
					connectionFactory,
					useDefaultHost,
					ValidationStatus.error(NLS.bind("Could not connect to host at {0}.", host), e),
					Status.OK_STATUS);
		}
		return factory;
	}
	
	private void setConnectionFactoryError(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTION_FACTORY_ERROR, this.connectionFactoryError, this.connectionFactoryError = status);
	}

	public IStatus getConnectionFactoryError() {
		return connectionFactoryError;
	}
	
	public void setUseDefaultHost(boolean useDefaultHost) {
		update(selectedConnection, connection, connectionFactory, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}
	
	public boolean isUseDefaultHost() {
		return useDefaultHost;
	}

	public IStatus getConnectionCreationError() {
		return connectionFactoryError;
	}

	public IStatus connect() {
		IStatus status = Status.OK_STATUS;
		try {			
			if(connection != null 
					&& !connection.connect()) {
				String message = String.format("Unable to connect to %s", connection.getHost());
				OpenShiftCommonUIActivator.log(message, null);
				status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, message);
			}
//		} catch (NotFoundOpenShiftException e) {
//			// connectionFactoryError user without domain
		} catch (Exception e) {
			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
					NLS.bind("Unknown error, can not verify connection to host {0} - see Error Log for details", connection.getHost()));
			OpenShiftCommonUIActivator.log(e);
		}
		update(selectedConnection, connection, connectionFactory, useDefaultHost, Status.OK_STATUS, status);
		return status;
	}
	
	private void setConnectError(IStatus status) {
		firePropertyChange(PROPERTY_CONNECT_ERROR, this.connectError, this.connectError = status);
	}

	public IStatus getConnectError() {
		return connectError;
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

	public Collection<IConnectionFactory> getAllConnectionFactories() {
		List<IConnectionFactory> connectionFactories = new ArrayList<IConnectionFactory>();
		connectionFactories.add(AutomaticConnectionFactoryMarker.getInstance());
		connectionFactories.addAll(connectionsFactory.getAll());
		return connectionFactories;
	}

	public void dispose() {
		connectionsFactory.close();
		ObservablePojoUtils.removePropertyChangeListener(this.connectionChangeListener, this.connection);
	}
	
	/**
	 * Either adds the connection that was created to the connection registry or
	 * updates the connection that was edited.
	 * 
	 * @return
	 */
	public boolean saveConnection() {
		if (connection == null
				|| !connectError.isOK()) {
			return false;
		}

		if (isNewConnection()) {
			ConnectionsRegistrySingleton.getInstance().add(connection);
		} else {
			getSelectedConnection().update(connection);
		}
		return true;
	}
}