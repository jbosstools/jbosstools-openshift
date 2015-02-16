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

	public static final String PROPERTY_CONNECTION = "connection";
	public static final String PROPERTY_CONNECTION_FACTORY = "connectionFactory";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_CONNECTION_CREATION_ERROR = "connectionCreationError";
	public static final String PROPERTY_USE_DEFAULT_HOST = "useDefaultHost";
	public static final String PROPERTY_CONNECT_ERROR = "connectError";

	private IConnection connection;
	private IStatus connectionCreationError;
	private String host;
	private boolean allowConnectionChange;
	private IConnectionFactory connectionFactory;
	private boolean useDefaultHost;
	private final ConnectionsFactoryTracker connectionsFactory;
	private final IConnectionAwareModel wizardModel;
	private IStatus connectError;
	private PropertyChangeListener connectionChangeListener;
	
	ConnectionWizardPageModel(IConnectionAwareModel wizardModel, boolean allowConnectionChange) {
		this.wizardModel = wizardModel;
		this.allowConnectionChange = allowConnectionChange;
		this.connectionsFactory = createConnectionsFactory();
		setUseDefaultHost(true);
		setConnectionFactory(connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_EXPRESS_ID), true);
		setConnection(wizardModel.getConnection(), true);
	}

	private ConnectionsFactoryTracker createConnectionsFactory() {
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		return connectionsFactory;
	}

	private void updateModel(IConnection connection, IConnectionFactory factory, IStatus connectionCreationError, IStatus connectError) {
		updateConnectionFactory(factory);
		updateConnection(connection, connectionCreationError, connectError);
	}

	private void updateConnectionFactory(IConnectionFactory factory) {
		if (factory != null
				&& !factory.equals(connectionFactory)) {
			if (isUseDefaultHost()) {
				setUseDefaultHost(factory.hasDefaultHost(), false);
				if (!StringUtils.isEmpty(factory.getDefaultHost())) {
					setHost(factory.getDefaultHost(), false);
				}
			}
		}
	}

	private void updateConnection(IConnection connection, IStatus connectionCreationError, IStatus connectError) {
		if (!equalsTypeAndHost(connection, this.connection)) {
			if (connection != null) {
				// switch from Automatic->specific server type
				setConnectionFactory(connectionsFactory.getByConnection(connection.getClass()), false);
			}
			setConnection(connection, false);
		}
		setConnectionCreationError(connectionCreationError);
		setConnectError(connectError);
	}

	public void setConnection(IConnection connection) {
		setConnection(connection, true);
	}

	private void setConnection(IConnection connection, boolean updateModel) {
		if (updateModel) {
			if (connection != null) {
				updateModel(connection, connectionFactory, Status.OK_STATUS, Status.OK_STATUS);
			} else {
				updateModel(connection, connectionFactory, Status.CANCEL_STATUS, Status.OK_STATUS);
			}
		}
		addConnectionListener(connection, this.connection);
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
	}
	
	private void addConnectionListener(IConnection newConnection, IConnection oldConnection) {
		if (!Diffs.equals(newConnection, oldConnection)) {
			ObservablePojoUtils.removePropertyChangeListener(this.connectionChangeListener, oldConnection);
			ObservablePojoUtils.addPropertyChangeListener(onConnectionChanged(), newConnection);
		}
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

	public IConnection getConnection() {
		return connection;
	}

	public void setConnectionFactory(IConnectionFactory factory) {
		setConnectionFactory(factory, true);
	}

	private void setConnectionFactory(IConnectionFactory factory, boolean updateModel) {
		if (updateModel) {
			updateModel(null, factory, Status.CANCEL_STATUS, Status.OK_STATUS);
		}
		firePropertyChange(PROPERTY_CONNECTION_FACTORY, this.connectionFactory, this.connectionFactory = factory);
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
		setHost(host, true);
	}

	private void setHost(String host, boolean updateModel) {
		if (updateModel) {
			updateModel(null, connectionFactory, Status.CANCEL_STATUS, Status.OK_STATUS);
		}
		firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
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
		if (connectionFactory == null
				|| StringUtils.isEmpty(host)) {
			return;
		}
		if (connectionFactory instanceof AutomaticConnectionFactoryMarker) {
			createConnection(host, connectionsFactory);
		} else {
			createConnection(host, connectionFactory);
		}
	}

	private void createConnection(String host, IConnectionFactory factory) {
		IConnection connection = null;
		try {
			connection = factory.create(host);
			if (equalsTypeAndHost(connection, this.connection)) {
				return;
			}
			
			if (connection.canConnect()) {
				updateModel(connection, 
						factory, 
						Status.OK_STATUS, 
						Status.OK_STATUS);
			} else {
				updateModel(null, 
						factory, 
						ValidationStatus.error(NLS.bind("Host at {0} is not an {1} host.", host, factory.getName())), 
						Status.OK_STATUS);
			}
		} catch (IOException e) {
			updateModel(null, 
					factory, 
					ValidationStatus.error(NLS.bind("Could not connect to host at {0}.", host), e), 
					Status.OK_STATUS);
		}
	}

	private void createConnection(String host, IConnectionsFactory connectionsFactory) {
		IConnection connection;
		try {
			connection = connectionsFactory.create(host);
			if (connection != null) {
				updateModel(connection, 
						connectionFactory, 
						Status.OK_STATUS,
						Status.OK_STATUS);
			} else {
				updateModel(null, 
						connectionFactory,
						ValidationStatus.error(NLS.bind("The host at {0} is no OpenShift host", host)),
						Status.OK_STATUS);
			}
		} catch (IOException e) {
			updateModel(null, 
					AutomaticConnectionFactoryMarker.getInstance(),
					ValidationStatus.error(NLS.bind("Could not connect to host at {0}.", host), e), 
					Status.OK_STATUS);
		}
	}

	private boolean equalsTypeAndHost(IConnection thisConnection, IConnection thatConnection) {
		if ((thisConnection == null && thatConnection != null)
				|| (thatConnection == null && thisConnection != null)) {
			return false;
		} else if (thisConnection == null && thatConnection == null) {
			return true;
		}
		return thisConnection.getType().equals(thatConnection.getType())
				&& thisConnection.getHost().equals(thatConnection.getHost());
	}
	
	public void setUseDefaultHost(boolean useDefaultHost) {
		setUseDefaultHost(useDefaultHost, true);
	}
	
	private void setUseDefaultHost(boolean useDefaultHost, boolean updateModel) {
		if (updateModel) {
			updateModel(null, connectionFactory, Status.CANCEL_STATUS, Status.OK_STATUS);
		}
		firePropertyChange(PROPERTY_USE_DEFAULT_HOST, this.useDefaultHost, this.useDefaultHost = useDefaultHost);
	}
	
	public boolean isUseDefaultHost() {
		return useDefaultHost;
	}

	private void setConnectionCreationError(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTION_CREATION_ERROR, this.connectionCreationError, this.connectionCreationError = status);
	}

	public IStatus getConnectionCreationError() {
		return connectionCreationError;
	}

	public IStatus connect() {
		IStatus status = Status.OK_STATUS;
		try {
			if(connection != null) {
				connection.connect();
			}
//		} catch (NotFoundOpenShiftException e) {
//			// connectionCreationError user without domain
		} catch (Exception e) {
			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
					NLS.bind("Unknown error, can not verify connection to host {0} - see Error Log for details", connection.getHost()));
			OpenShiftCommonUIActivator.log(e);
		}
		setConnectError(status);
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
}