/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core;

import java.util.Collection;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionPersistency;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.server.resources.OpenshiftResourceChangeListener;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCoreActivator extends BaseCorePlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.core"; //$NON-NLS-1$
	private static OpenShiftCoreActivator instance;
	private IServerLifecycleListener serverListener;
	private OpenshiftResourceChangeListener resourceChangeListener;
	public OpenShiftCoreActivator() {
		super();
		instance = this;
	}

	public static OpenShiftCoreActivator getDefault() {
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
        registerDebugOptionsListener(PLUGIN_ID, new Trace(this), context);
        Collection<Connection> connections = new ConnectionPersistency().load();
        ConnectionsRegistrySingleton.getInstance().addAll(connections);
        ConnectionsRegistrySingleton.getInstance().addListener(new ConnectionsRegistryAdapter() {
        	
        	//@TODO I think we need to handle the cleanup case where a connection
            //username changes since username is what makes up part of the
        	//the key that is saved which seems to apply to secure values
        	//and preference values
        	
        	
			@Override
			public void connectionRemoved(IConnection connection) {
				if (!(connection instanceof Connection)) {
					return;
				}
				((Connection)connection).removeSecureStoreData();
				ConnectionURL url = ConnectionURL.safeForConnection(connection);
				if(url != null) {
					OpenShiftCorePreferences.INSTANCE.removeAuthScheme(url.toString());
				}
				saveAllConnections();

			}
			
			@Override
			public void connectionAdded(IConnection connection) {
				if (connection instanceof Connection) {
					saveAllConnections();
				}
			}
        	
			@Override
			public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
				if (connection instanceof Connection && (oldValue instanceof Connection || newValue instanceof Connection) ) {
					saveAllConnections();
				}
			}
        });
        ServerCore.addServerLifecycleListener(getServerListener());
        // A clone of the auto-publish thread implementation
        resourceChangeListener = new OpenshiftResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

	}

    @Override
	public void stop(BundleContext context) throws Exception {
    	saveAllConnections();
    	ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
    	ServerCore.removeServerLifecycleListener(getServerListener());
    	super.stop(context);
	}

	private IServerLifecycleListener getServerListener() {
		if( serverListener == null ) {
			serverListener = new IServerLifecycleListener() {
				@Override
				public void serverRemoved(IServer server) {
				}
				@Override
				public void serverChanged(IServer server) {
				}
				@Override
				public void serverAdded(IServer server) {
					if( server != null ) {
						String typeId = server.getServerType().getId();
						if( OpenShiftServer.SERVER_TYPE_ID.equals(typeId)) {
							if( server.getAttribute(OpenShiftServerUtils.SERVER_START_ON_CREATION, false)) {
								try {
									server.start("run", new NullProgressMonitor());
								} catch(CoreException ce) {
									pluginLog().logError("Error starting server", ce);
								}
							}
						}
					}
				}
			};
		}
		return serverListener;
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

	public static void logError(String message, Throwable t) {
		pluginLog().logError(message, t);
	}
	
	public static void logWarning(String message, Throwable t) {
		pluginLog().logWarning(message, t);
	}
	
	/**
	 * Get a status factory for this plugin
	 * @return status factory
	 */
	public static StatusFactory statusFactory() {
		return getDefault().statusFactoryInternal();
	}

	protected void saveAllConnections() {
		Collection<Connection> connections = ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
		new ConnectionPersistency().save(connections);
	}

}
