/*******************************************************************************
 * Copyright (c) 2012-2016 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsFactoryTracker;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.core.security.SecureStoreException;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @contributor Nick Boldt
 * @author Jeff Maury
 */
public class ConnectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_CONNECTION = "selectedConnection";
	public static final String PROPERTY_CONNECTION_FACTORY = "connectionFactory";
	public static final String PROPERTY_CONNECTION_FACTORY_ERROR = "connectionFactoryError";
	public static final String PROPERTY_HOST = "host";
	public static final String PROPERTY_USE_DEFAULT_HOST = "useDefaultHost";
	public static final String PROPERTY_ALL_HOSTS = "allHosts";
	public static final String PROPERTY_CONNECTED_STATUS = "connectedStatus";
	public static final String PROPERTY_SIGNUPURL = "signupUrl";
	public static final String PROPERTY_USERDOCURL = "userdocUrl";
	public static final String PROPERTY_HAS_DEFAULT_HOST = "hasDefaultHost";
	private static final IStatus NOT_CONNECTED_STATUS = null;
	
	/** the connection that the user wants to edit */
	private IConnection selectedConnection;
	/** the connection that this wizard operates on */
	private IConnection connection;
	private IStatus connectionFactoryError;
	private String host;
	private IConnectionFactory connectionFactory;
	private boolean useDefaultHost;
	private boolean hasDefaultHost;
	private Collection<String> allHosts;
	private ConnectionsFactoryTracker connectionsFactory;
	private String signupUrl;
	private String userdocUrl;
	private IStatus connectedStatus;
	private IConnectionAuthenticationProvider connectionAuthenticationProvider;
	private Collection<IConnection> allConnections;
	private Class<? extends IConnection> connectionType;
	private IConnectionAware<IConnection> wizardModel;
	private boolean enablePromptCredentialsBackup = false;
	
	protected ConnectionWizardPageModel(IConnection editedConnection, Collection<IConnection> allConnections, 
			Class<? extends IConnection> connectionType, boolean allowConnectionChange, IConnectionAware<IConnection> wizardModel) {
		this.allConnections = filterAllConnections(editedConnection, allConnections, connectionType, allowConnectionChange);
		this.connectionType = connectionType;
		this.wizardModel = wizardModel;
		this.allHosts = createAllHosts(allConnections);
		this.connectionsFactory = createConnectionsFactory();
		init(editedConnection, connectionType, connectionFactory);
	}

	private Collection<IConnection> filterAllConnections(IConnection editedConnection, Collection<IConnection> allConnections,
			Class<? extends IConnection> connectionType, boolean allowConnectionChange) {
		if (!allowConnectionChange) {
			return Collections.singletonList(editedConnection);
		} else if (connectionType == null) {
			return allConnections;
		} else {
			return allConnections.stream()
						.filter(connection -> connection.getClass().equals(connectionType))
						.collect(Collectors.toList());			
		}
	}

	private Collection<String> createAllHosts(Collection<IConnection> allConnections) {
		if (allConnections == null) {
			return Collections.emptyList();
		}

		return allConnections.stream()
				.map(connection -> connection.getHost())
				.filter(host -> !StringUtils.isEmpty(host))
				.distinct()
				.collect(Collectors.toList());
	}

	protected ConnectionsFactoryTracker createConnectionsFactory() {
		ConnectionsFactoryTracker connectionsFactory = new ConnectionsFactoryTracker();
		connectionsFactory.open();
		return connectionsFactory;
	}
	
	private void init(IConnection editedConnection, Class<? extends IConnection> connectionType, IConnectionFactory connectionFactory) {
		this.connectedStatus = Status.OK_STATUS;
		this.connectionFactoryError = Status.OK_STATUS;
		initConnection(editedConnection, connectionType);
		this.signupUrl = getSignupUrl(host, this.connectionFactory);
		this.userdocUrl = getUserdocUrl(this.connectionFactory);
	}

	private void initConnection(IConnection editedConnection, Class<? extends IConnection> connectionType) {
		if (editedConnection == null
				|| (connectionType != null && !editedConnection.getClass().equals(connectionType))) {
			initNewConnection(connectionType);
		} else {
			initEditConnection(editedConnection);
		}
	}

	private void initEditConnection(IConnection connection) {
		this.selectedConnection = connection;
		this.enablePromptCredentialsBackup = selectedConnection.isEnablePromptCredentials();
		this.selectedConnection.enablePromptCredentials(false);
		this.connectionFactory = connectionsFactory.getByConnection(connection.getClass());
		this.host = connection.getHost();
		this.hasDefaultHost = connectionFactory != null && connectionFactory.hasDefaultHost();
		this.useDefaultHost = connection.isDefaultHost();
	}

	private void initNewConnection(Class<? extends IConnection> connectionType) {
		this.selectedConnection = NewConnectionMarker.getInstance();
		this.connectionFactory = getConnectionFactory(connectionType, connectionsFactory);
		if (connectionFactory != null) {
			this.host = connectionFactory.getDefaultHost();
			this.useDefaultHost = this.hasDefaultHost = connectionFactory.hasDefaultHost();
		}
	}

	private IConnectionFactory getConnectionFactory(Class<? extends IConnection> connectionType, IConnectionsFactory connectionsFactory) {
		IConnectionFactory factory = null;
		if (connectionType == null) {
			factory = getDefaultConnectionFactory(connectionsFactory);
		} else{
			factory = connectionsFactory.getByConnection(connectionType);
		}
		return factory;
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
		String userdocUrl = getUserdocUrl(factory);

		firePropertyChange(PROPERTY_SELECTED_CONNECTION, this.selectedConnection, this.selectedConnection = selectedConnection);
		firePropertyChange(PROPERTY_CONNECTION_FACTORY, this.connectionFactory, this.connectionFactory = factory);
		firePropertyChange(PROPERTY_HOST, this.host, this.host = host);
		firePropertyChange(PROPERTY_USE_DEFAULT_HOST, this.useDefaultHost, this.useDefaultHost = useDefaultHost);
		firePropertyChange(PROPERTY_HAS_DEFAULT_HOST, this.hasDefaultHost, this.hasDefaultHost = factory != null && factory.hasDefaultHost());
		firePropertyChange(PROPERTY_SIGNUPURL, this.signupUrl, this.signupUrl = signupUrl);
		firePropertyChange(PROPERTY_USERDOCURL, this.userdocUrl, this.userdocUrl = userdocUrl);
		
		setConnectionFactoryError(connectionFactoryError);
		setConnectedStatus(connectError);
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
		if (!selectedConnection.equals(this.selectedConnection)) {
			// changed connection
			if (selectedConnection instanceof NewConnectionMarker) {
				host = factory.getDefaultHost();
			} else {
				host = selectedConnection.getHost();
			}
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
		if (selectedConnection != null
			&& !selectedConnection.equals(this.selectedConnection)) {
			// connection changed
			if (selectedConnection instanceof NewConnectionMarker) {
				// <New Connection> selected
				useDefaultHost = factory.hasDefaultHost();
			} else {
				// existing connection selected
				useDefaultHost = selectedConnection.isDefaultHost();
			}
		} else if (factory != null
				&& !factory.equals(connectionFactory)) {
			// server type changed
			if (useDefaultHost
					|| StringUtils.isEmpty(this.host)) {
				useDefaultHost = factory.hasDefaultHost();
			}
		} else if (useDefaultHost) {
			// use default host clicked
			if (!factory.hasDefaultHost()) {
				useDefaultHost = false;
			}
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

	private String getUserdocUrl(IConnectionFactory factory) {
		if (factory == null) {
			return null;
		}
		return factory.getUserDocUrl();
	}

	protected boolean isNewConnection() {
		return selectedConnection instanceof NewConnectionMarker;
	}
	
	public void setSelectedConnection(IConnection selectedConnection) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, NOT_CONNECTED_STATUS);
	}

	public IConnection getSelectedConnection() {
		return selectedConnection;
	}

	public IConnection getConnection() {
		return connection;
	}
	
	public void setConnectionFactory(IConnectionFactory factory) {
		update(NewConnectionMarker.getInstance(), factory, host, useDefaultHost, Status.OK_STATUS, NOT_CONNECTED_STATUS);
	}
	
	public IConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public boolean isHasDefaultHost() {
		return hasDefaultHost;
	}
	
	public Collection<IConnection> getAllConnections() {
		List<IConnection> connections = new ArrayList<>();
		connections.add(NewConnectionMarker.getInstance());
		connections.addAll(allConnections);
		return connections;
	}

	public String getHost() {
		return host;
	}

	public String getSignupUrl() {
		return signupUrl;
	}
	
	public String getUserdocUrl() {
		return userdocUrl;
	}
	
	public void setHost(String host) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, NOT_CONNECTED_STATUS);
	}
	
	private void setConnectionFactoryError(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTION_FACTORY_ERROR, this.connectionFactoryError, this.connectionFactoryError = status);
	}

	public IStatus getConnectionFactoryError() {
		return connectionFactoryError;
	}
	
	public void setUseDefaultHost(boolean useDefaultHost) {
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, NOT_CONNECTED_STATUS);
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
		if (isConnected() 
				&& listener.secureStoreException == null) {
			return Status.OK_STATUS;
		}
		IStatus status = Status.OK_STATUS;
		listener.secureStoreException = null;
		try {
			IConnection connection = 
					isNewConnection()
					? createConnection(connectionFactory, connectionAuthenticationProvider)
					: this.selectedConnection;
			if(connection != null) {
				addConnectionListener(connection);	
				if (connection.connect()) {
					connection.enablePromptCredentials(true);
					this.connection = connection;
					wizardModel.setConnection(connection);
					ConnectionsRegistrySingleton.getInstance().setRecent(connection);
					connection.notifyUsage();
				} else {
					String message = NLS.bind("Unable to connect to {0}", connection.getHost());
					OpenShiftCommonUIActivator.log(message, null);
					status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID, message);
				}
			}
		} catch (Exception e) {
			status = StatusFactory.errorStatus(OpenShiftCommonUIActivator.PLUGIN_ID,
					                           e.getMessage());
			OpenShiftCommonUIActivator.log(e);
		} finally {
			removeConnectionListener(connection);
		}
		update(selectedConnection, connectionFactory, host, useDefaultHost, Status.OK_STATUS, status);
		return status;
	}

	private void removeConnectionListener(IConnection connection) {
		if(connection instanceof IObservablePojo) {
			((IObservablePojo)connection).removePropertyChangeListener(listener);
		}
	}

	private void addConnectionListener(IConnection connection) {
		if(connection instanceof IObservablePojo) {
			((IObservablePojo)connection).addPropertyChangeListener(listener);
		}
	}

	StorageAccessListener listener = new StorageAccessListener();
	
	class StorageAccessListener implements PropertyChangeListener {
		SecureStoreException secureStoreException = null;
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if(SecureStoreException.ID.equals(evt.getPropertyName()) && evt.getNewValue() instanceof SecureStoreException) {
				secureStoreException = (SecureStoreException)evt.getNewValue();
			}			
		}
	}

	SecureStoreException getRecentSecureStoreException() {
		return listener.secureStoreException;
	}

	private IConnection createConnection(IConnectionFactory factory, IConnectionAuthenticationProvider authProvider) {
		if (factory == null) {
			return null;
		}
		
		IConnection connection = factory.create(getHost());
		if (connection == null) {
			return null;
		}
		
		if (authProvider != null) {
			authProvider.update(connection);
		}
		connection.enablePromptCredentials(false);
		return connection;
	}

	public void setNotConnected() {
		setConnectedStatus(NOT_CONNECTED_STATUS);
	}
	
	private void setConnectedStatus(IStatus status) {
		firePropertyChange(PROPERTY_CONNECTED_STATUS, this.connectedStatus, this.connectedStatus = status);
		if (isConnected()) {
			this.connection.enablePromptCredentials(true);
		}
	}

	public IStatus getConnectedStatus() {
		return connectedStatus;
	}

	/**
	 * Returns {@code true} if the connection in this model is already
	 * connected. Returns {@code false} otherwise. The reason for implementing
	 * this in the wizard and not in the connection is that you cannot be sure
	 * of a connected state since tokens have an expiration timestamp. This
	 * method thus is only of limited use within a wizard that doesn't want to
	 * re-connect a connection that it created upon page changes etc.
	 * 
	 * @return true if the
	 */
	public boolean isConnected() {
		return connectedStatus != null
				&& connectedStatus != NOT_CONNECTED_STATUS
				&& connectedStatus.isOK();
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
		return connectionsFactory.getAll(connectionType);
	}

	@Override
	public void dispose() {
		connectionsFactory.close();
		if (this.selectedConnection != null) {
			selectedConnection.enablePromptCredentials(enablePromptCredentialsBackup);
		}
	}
	
	/**
	 * Either adds the connection that was created to the connection registry or
	 * updates the connection that was edited.
	 * 
	 * @return
	 */
	public boolean saveConnection() {
		if (connection == null
				|| !connectedStatus.isOK()) {
			return false;
		}

		this.connection.enablePromptCredentials(true);

		if (isNewConnection()) {
			ConnectionsRegistrySingleton.getInstance().add(connection);
		} else {
			ConnectionsRegistrySingleton.getInstance().update(getSelectedConnection(), connection);
		}

		return true;
	}

	public void cancel() {
		this.connection.enablePromptCredentials(true);
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

	public static interface IConnectionFilter {

		public boolean accept(IConnection connection); 
	}

}