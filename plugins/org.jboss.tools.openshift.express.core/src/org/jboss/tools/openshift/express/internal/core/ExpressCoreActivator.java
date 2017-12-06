/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.express.internal.core;

import java.util.Collection;

import org.eclipse.core.runtime.Platform;
import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnectionPersistency;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 */
public class ExpressCoreActivator extends BaseCorePlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.express.core"; //$NON-NLS-1$

	private static ExpressCoreActivator instance;

	public ExpressCoreActivator() {
		super();
		instance = this;
	}

	public static ExpressCoreActivator getDefault() {
		return instance;
	}

	public static BundleContext getBundleContext() {
		if (instance == null) {
			return null;
		}
		return instance.getBundleContext();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		ConnectionsRegistrySingleton.getInstance().addAll(new ExpressConnectionPersistency().load());

		ConnectionsRegistrySingleton.getInstance().addListener(new ConnectionsRegistryAdapter() {
			@Override
			public void connectionRemoved(IConnection connection) {
				if (connection instanceof ExpressConnection) {
					((ExpressConnection) connection).removeSecureStoreData();
					saveAllConnections();
				}
			}

			@Override
			public void connectionAdded(IConnection connection) {
				if (connection instanceof ExpressConnection) {
					saveAllConnections();
				}
			}

			@Override
			public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
				if (connection instanceof ExpressConnection
						&& (oldValue instanceof ExpressConnection || newValue instanceof ExpressConnection)) {
					saveAllConnections();
				}
			}

		});
	}

	/**
	 * Gets message from plugin.properties
	 * @param key
	 * @return
	 */
	public static String getMessage(String key) {
		return Platform.getResourceString(instance.getBundle(), key);
	}

	/**
	 * Get the IPluginLog for this plugin. This method 
	 * helps to make logging easier, for example:
	 * 
	 *     FoundationCorePlugin.pluginLog().logError(etc)
	 *  
	 * @return IPluginLog object
	 */
	public static IPluginLog pluginLog() {
		return getDefault().pluginLogInternal();
	}

	/**
	 * Get a status factory for this plugin
	 * @return status factory
	 */
	public static StatusFactory statusFactory() {
		return getDefault().statusFactoryInternal();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		new ExpressConnectionPersistency()
				.save(ConnectionsRegistrySingleton.getInstance().getAll(ExpressConnection.class));

		super.stop(context);
		context = null;
	}

	protected void saveAllConnections() {
		Collection<ExpressConnection> connections = ConnectionsRegistrySingleton.getInstance()
				.getAll(ExpressConnection.class);
		new ExpressConnectionPersistency().save(connections);
	}
}
