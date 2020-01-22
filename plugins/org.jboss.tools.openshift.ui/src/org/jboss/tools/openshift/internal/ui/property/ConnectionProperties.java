/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import static org.jboss.tools.openshift.internal.ui.property.ConnectionPropertySource.KUBERNETES_MASTER_VERSION;
import static org.jboss.tools.openshift.internal.ui.property.ConnectionPropertySource.OPENSHIFT_MASTER_VERSION;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;

/**
 * A provider for connection properties. Loads and caches connection properties.
 */
class ConnectionProperties {
	
	private static final ConnectionProperties instance = new ConnectionProperties(); 
	
	public static ConnectionProperties getInstance() {
		return instance;
	}

	private Map<IOpenShiftConnection, AsyncProvider> providers = new ConcurrentHashMap<>();

	private ConnectionProperties() {
		// no instantiation
	}

	public String getProperty(Object id, IOpenShiftConnection connection, Consumer<String> loadedCallback) {
		AsyncProvider loader = providers.computeIfAbsent(connection, this::createAsyncProvider);
		return loader.getProperty(id, loadedCallback);
	}

	public boolean supports(Object id) {
		return KUBERNETES_MASTER_VERSION.equals(id) 
				|| OPENSHIFT_MASTER_VERSION.equals(id);
	}

	private AsyncProvider createAsyncProvider(IOpenShiftConnection connection) {
		AsyncProvider cache = new AsyncProvider(connection);
		ConnectionsRegistrySingleton.getInstance().addListener(onConnectionRemoved());
		return cache;
	}

	private ConnectionsRegistryAdapter onConnectionRemoved() {
		return new ConnectionsRegistryAdapter() {

			@Override
			public void connectionRemoved(IConnection connection) {
				providers.remove(connection);
			}
		};
	}

	/**
	 * A provider that loads and caches properties for a given connection. 
	 *
	 */
	private static class AsyncProvider {
		private enum State {
			NOT_LOADED, LOADING, LOADED, ERROR
		}

		private static final String LOADING_VALUE = "Loading...";
		private static final String ERROR_VALUE = "Error retrieving property (check logs)";

		private IOpenShiftConnection connection;
		private AtomicReference<State> state = new AtomicReference<>(State.NOT_LOADED);
		private String kubernetesMasterVersion;
		private String openShiftMasterVersion;

		private AsyncProvider(IOpenShiftConnection connection) {
			this.connection = connection;
		}

		public String getProperty(Object id, Consumer<String> loadedCallback) {
			switch(state.get()) {
				case NOT_LOADED:
					return loadProperties(id, loadedCallback);
				case LOADING:
					return LOADING_VALUE;
				default:
				case LOADED: 
					return getLoadedProperty(id);
				case ERROR:
					return ERROR_VALUE;
			}
		}

		private String getLoadedProperty(Object id) {
			if (KUBERNETES_MASTER_VERSION.equals(id)) {
				return kubernetesMasterVersion;
			} else if (OPENSHIFT_MASTER_VERSION.equals(id)) {
				return openShiftMasterVersion;
			} else {
				return null;
			}
		}

		private String loadProperties(Object id, Consumer<String> onLoaded) {
			state.set(State.LOADING);
			Job.create(NLS.bind("Loading properties for connection {0}...", 
					connection.getHost()),
					load(id, onLoaded))
				.schedule();
			return LOADING_VALUE;
		}

		private ICoreRunnable load(Object id, Consumer<String> onLoaded) {
			return (IProgressMonitor monitor) -> {
				if (!(connection instanceof IOpenShiftConnection)) {
					return;
				}
				IOpenShiftConnection osConnection = (IOpenShiftConnection) connection;
				try {
					kubernetesMasterVersion = osConnection.getKubernetesMasterVersion();
					openShiftMasterVersion = osConnection.getOpenShiftMasterVersion();
					state.set(State.LOADED);
				} catch (OpenShiftException e) {
					kubernetesMasterVersion = null;
					openShiftMasterVersion = null;
					state.set(State.ERROR);
					OpenShiftUIActivator.log(IStatus.ERROR,
							"Could not load openshift- and kubernetes master versions.", e);
				} finally {
					onLoaded.accept(getLoadedProperty(id));
				}
			};
		}
	}
}