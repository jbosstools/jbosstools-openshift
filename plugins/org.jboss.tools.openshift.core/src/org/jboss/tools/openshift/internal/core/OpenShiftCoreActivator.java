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

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionSerializer;
import org.jboss.tools.openshift.core.preferences.OpenShiftPreferences;
import org.osgi.framework.BundleContext;

import com.openshift.client.OpenShiftException;

/**
 * The activator class controls the plug-in life cycle
 */
public class OpenShiftCoreActivator extends BaseCorePlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.openshift.core"; //$NON-NLS-1$
	private static OpenShiftCoreActivator instance;
	private static BundleContext myContext;
	
	public OpenShiftCoreActivator() {
		super();
		instance = this;
	}

	public static OpenShiftCoreActivator getDefault() {
	    return instance;
	}

	public static BundleContext getBundleContext() {
	    return myContext;
	}

	@Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        myContext = context;
        String[] connections = OpenShiftPreferences.getInstance().getConnections();
        ConnectionSerializer serializer = new ConnectionSerializer();
        for (String entry : connections) {
        	try{
        		Connection connection = serializer.deserialize(entry);
				ConnectionsRegistrySingleton.getInstance().add(connection);
    		}catch(OpenShiftException e){
    			pluginLog().logError(String.format("Exception will trying to deserialize the connection '%s'", entry), e);
    		}
		}
	}
	

    @Override
	public void stop(BundleContext context) throws Exception {
    	Collection<Connection> all = ConnectionsRegistrySingleton.getInstance().getAll(Connection.class);
    	ConnectionSerializer serializer = new ConnectionSerializer();
    	Collection<String> connections = new ArrayList<String>(all.size());
    	for (Connection connection : all) {
    		try{
    			connections.add(serializer.serialize(connection));
    		}catch(OpenShiftException e){
    			pluginLog().logError(String.format("Exception will trying to serialize the connection '%s'",connection), e);
    		}
		}
    	OpenShiftPreferences.getInstance().saveConnections(connections.toArray(new String []{}));
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

	/**
	 * Get a status factory for this plugin
	 * @return status factory
	 */
	public static StatusFactory statusFactory() {
		return getDefault().statusFactoryInternal();
	}

}
