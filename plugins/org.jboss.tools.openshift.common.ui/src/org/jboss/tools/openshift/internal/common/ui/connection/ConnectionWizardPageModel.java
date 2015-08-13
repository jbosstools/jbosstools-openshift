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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_CONNECTION = "selectedConnection";
	public static final String PROPERTY_CONNECTION_FACTORY = "connectionFactory";
	public static final String PROPERTY_CONNECTION_FACTORY_ERROR = "connectionFactoryError";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_USE_DEFAULT_HOST = "useDefaultHost";
	public static final String PROPERTY_ALL_HOSTS = "allHosts";
	public static final String PROPERTY_CONNECT_ERROR = "connectError";
	public static final String PROPERTY_SIGNUPURL = "signupUrl";

	/** the connection that the user wants to edit */
	private IConnection selectedConnection;
	/** the connection that this wizard operates on */
	private IConnection connection;
	private IStatus connectionFactoryError;
	private String host;
	private boolean allowConnectionChange;
	private IConnectionFactory connectionFactory;
	private boolean useDefaultHost;
	private Collection<String> allHosts;
	private ConnectionsFactoryTracker connectionsFactory;
	private String signupUrl;
	private IStatus connectError;
	private IConnectionAuthenticationProvider connectionAuthenticationProvider;
	private Collection<IConnection> allConnections;
	private IConnectionAware<IConnection> wizardModel;
	
	ConnectionWizardPageModel(IConnection editedConnection, Collection<IConnection> allConnections, boolean allowConnectionChange, IConnectionAware<IConnection> wizardModel) {
		this.allConnections = allConnections;
		this.wizardModel = wizardModel;
		this.allHosts = createAllHosts(allConnections);
		this.allowConnectionChange = allowConnectionChange;
		this.connectionsFactory = createConnectionsFactory();
		init(editedConnection);
	}

	private Collection<String> createAllHosts(Collection<IConnection> allConnections) {
		List<String> allHosts = new ArrayList<String>();
		if (allConnections == null) {
			return allHosts;
		}
		
		for (IConnection connection : allConnections) {
			if (!StringUtils.isEmpty(connection.getHost())) {
				allHosts.add(connection.getHost());
			}
		}
		return allHosts;
	}

	private ConnectionsFactoryTracker createConnectionsFactory() {
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		return connectionsFactory;
	}
	
	private void init(IConnection editedConnection) {
		this.connectError = Status.OK_STATUS;
		this.connectionFactoryError = Status.OK_STATUS;
		if (editedConnection == null) {
			initNewConnection();
		} else {
			initEditConnection(editedConnection);
		}
		this.signupUrl = getSignupUrl(host, connectionFactory);
	}

	private void initEditConnection(IConnection connection) {
		this.selectedConnection = connection;
		this.connection = connection.clone();
		this.connectionFactory = connectionsFactory.getByConnection(connection.getClass());
		this.host = connection.getHost();
		this.useDefaultHost = connection.isDefaultHost();
	}

	private void initNewConnection() {
		this.selectedConnection = NewConnectionMarker.getInstance();
		this.connectionFactory = getDefaultConnectionFactory(connectionsFactory);
		if (connectionFactory != null) {
			this.host = connectionFactory.getDefaultHost();
			this.useDefaultHost = connectionFactory.hasDefaultHost();
			if (host != null) {
				this.connection = connectionFactory.create(host);
			}
		}
	}

	private IConnectionFactory getDefaultConnectionFactory(IConnectionsFactory connectionsFactory) {
		IConnectionFactory factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_EXPRESS_ID);
		if (factory == null) {
			factory = connectionsFactory.getById(IConnectionsFactory.CONNECTIONFACTORY_OPENSHIFT_ID);
		}
		return factory;
	}
	
	private void update(IConnection selectedConnection, IConnectionFactory factory, String host, boolean useDefaultHost, IStatus connectionFactoryError, IStatus connectError) {
		factory = updateFactory(factory, selectedConnection);
		useDefaultHost = updateUseDefaultHost(useDefaultHost, selectedConnection, factory);
		host = updateHost(host, useDefaultHost, selectedConnection, factory);
		String signupUrl = getSignupUrl(host, factory);

		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = selectedConnection);
		firePropertyChange(PROPERTY_CONNECTION_FACTORY, this.connectionFactory, this.connectionFactory = factory);
		firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
		firePropertyChange(PROPERTY_USE_DEFAULT_HOST, this.useDefaultHost, this.useDefaultHost = useDefaultHost);
		firePropertyChange(PROPERTY_SIGNUPURL, this.signupUrl, this.signupUrl = signupUrl);
		
		setConnectionFactoryError(connectionFactoryError);
		setConnectError(connectError);
	}

	private IConnectionFactory updateFactory(IConnectionFactory factory, IConnection selectedConnection) {
		if (selectedConnection instanceof NewConnectionMarker) {
			return factory;
		}
		
		if (!selectedConnection.equals(this.selectedConnection)) {
			// selectedConnection changed
			if (!(selectedConnection instanceof NewConnectionMarker)) {
				factory = connectionsFactory.getByConnection(selectedConnection.getClass());
			}
		}
		return factory;
	}

	private String updateHost(String host, boolean useDefaultHost, IConnection selectedConnection, IConnectionFactory factory) {
		if (!selectedConnection.equals(this.selectedConnection)
				&& useDefaultHost) {
			// changed connection
			host = selectedConnection.getHost();
		} else if (!factory.equals(this.connectionFactory)
				&& useDefaultHost) {
			// selected other server type
			host = factory.getDefaultHost();
		} else if (useDefaultHost != this.useDefaultHost) {
			// checked/unchecked "use default server"
			host = factory.getDefaultHost();
		}

		return host;
	}

	private boolean updateUseDefaultHost(boolean useDefaultHost, IConnection selectedConnection, IConnectionFactory factory) {
		if (factory != null
				&& !factory.equals(connectionFactory)) {
			// server type changed
			if (useDefaultHost
					|| StringUtils.isEmpty(this.host)) {
				useDefaultHost = factory.hasDefaultHost();
			}
		} else if (selectedConnection != null
				&& !selectedConnection.equals(this.selectedConnection)) {
				// connection changed
				useDefaultHost = selectedConnection.isDefaultHost();
		};
		return useDefaultHost;
	}

	private String getSignupUrl(String host, IConnectionFactory factory) {
		if (factory == null
				|| StringUtils.isEmpty(host)) {
			return null;
		}

		return factory.getSignupUrl(host);
	}

	private boolean isNewConnection() {
		return selectedConnection instanceof NewConnectionMarker;
	}
	
	public void setSelectedConnection(IConnection selectedConnection) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}

	public IConnection getSelectedConnection() {
		return selectedConnection;
	}

	public IConnection getConnection() {
		return connection;
	}
	
	public void setConnectionFactory(IConnectionFactory factory) {
		update(NewConnectionMarker.getInstance(), factory, host, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}
	
	public IConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
	
	public Collection<IConnection> getAllConnections() {
		if (allowConnectionChange) {
			List<IConnection> connections = new ArrayList<IConnection>();
			connections.add(NewConnectionMarker.getInstance());
			connections.addAll(allConnections);
			return connections;
		} else {
			return Collections.singletonList(selectedConnection);
		}
	}

	public String getHost() {
		return host;
	}

	public String getSignupUrl() {
		return signupUrl;
	}
	
	public void setHost(String host) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}
	
	private void setConnectionFactoryError(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTION_FACTORY_ERROR, this.connectionFactoryError, this.connectionFactoryError = status);
	}

	public IStatus getConnectionFactoryError() {
		return connectionFactoryError;
	}
	
	public void setUseDefaultHost(boolean useDefaultHost) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, Status.OK_STATUS);
	}
	
	public boolean isUseDefaultHost() {
		return useDefaultHost;
	}

	public void setAllHosts(Collection<String> allHosts) {
		firePropertyChange(PROPERTY_ALL_HOSTS, this.allHosts, this.allHosts = allHosts);
	}
	
	public Collection<String> getAllHosts() {
		return allHosts;
	}

	public IStatus connect() {
		IStatus status = Status.OK_STATUS;
		try {
			this.connection = createConnection();
			if(connection != null) {
				if (connection.connect()) {
					wizardModel.setConnection(connection);
					connection.notifyUsage();
				} else {
					String message = NLS.bind("Unable to connect to {0}", connection.getHost());
					OpenShiftCommonUIActivator.log(message, null);
					status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, message);
				}
			}
		} catch (Exception e) {
			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
					NLS.bind("The server type, credentials, or auth scheme might be incorrect. {0}", e.getMessage()));
			OpenShiftCommonUIActivator.log(e);
		}
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, status);
		return status;
	}
	
	private IConnection createConnection() {
		if (connectionFactory == null) {
			return null;
		}
		
		IConnection connection = connectionFactory.create(getHost());
		if (connection == null) {
			return null;
		}
		
		if (connectionAuthenticationProvider != null) {
			connectionAuthenticationProvider.update(connection);
		}
		return connection;
	}

	public void resetConnectError() {
		setConnectError(Status.OK_STATUS);
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
		connectionFactories.addAll(connectionsFactory.getAll());
		return connectionFactories;
	}

	public void dispose() {
		connectionsFactory.close();
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

	public void setConnectionAuthenticationProvider(IConnectionAuthenticationProvider authenticationProvider) {
		this.connectionAuthenticationProvider = authenticationProvider;
	}

	public interface IConnectionAuthenticationProvider {

		public IConnection update(IConnection connection);

	}
	
	public Object getContext() {
		return wizardModel.getContext();
	}
}