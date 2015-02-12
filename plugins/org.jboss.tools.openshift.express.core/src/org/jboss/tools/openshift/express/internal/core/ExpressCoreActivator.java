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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 */
public class ExpressCoreActivator extends BaseCorePlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.express.core"; //$NON-NLS-1$
	
	private static ExpressCoreActivator instance;
	private static BundleContext myContext;
	
	public ExpressCoreActivator() {
		super();
		instance = this;
	}

	public static ExpressCoreActivator getDefault() {
	    return instance;
	}

	public static BundleContext getBundleContext() {
	    return myContext;
	}

    public void start(BundleContext context) throws Exception {
        super.start(context);
        myContext = context;
        loadConnections();
	}
    
	private void loadConnections() {
		for (String url : ExpressCorePreferences.INSTANCE.getConnections()) {
			ConnectionsRegistrySingleton.getInstance().add(createConnection(url));
		}
	}
	
	private ExpressConnection createConnection(String url) {
		try {
			ConnectionURL connectionURL = ConnectionURL.forURL(url);
			return new ExpressConnection(connectionURL.getUsername(), connectionURL.getScheme(), connectionURL.getHost(), null, null);
		} catch (MalformedURLException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", url), e);
		} catch (UnsupportedEncodingException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", url), e);
		} catch (IllegalArgumentException e) {
			ExpressCoreActivator.pluginLog().logError(NLS.bind("Could not add connection for {0}.", url), e);
		}
		return null;
	}
	
	/**
	 * Gets message from plugin.properties
	 * @param key
	 * @return
	 */
	public static String getMessage(String key)	{
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

}
