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

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionPersistency;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCoreActivator extends BaseCorePlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.core"; //$NON-NLS-1$
	private static OpenShiftCoreActivator instance;
	private static BundleContext context;
	
	public OpenShiftCoreActivator() {
		super();
		instance = this;
	}

	public static OpenShiftCoreActivator getDefault() {
	    return instance;
	}

	public static BundleContext getBundleContext() {
	    return context;
	}

	@Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        this.context = context;
        Collection<Connection> connections = new ConnectionPersistency().load();
        ConnectionsRegistrySingleton.getInstance().addAll(connections);
	}

    @Override
	public void stop(BundleContext context) throws Exception {
    	Collection<Connection> connections = ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
		new ConnectionPersistency().save(connections);
    	super.stop(context);
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

}
