/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.common.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class OpenShiftCommonCoreActivator extends BaseCorePlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jboss.tools.openshift.common.core"; //$NON-NLS-1$
    private static OpenShiftCommonCoreActivator instance;
    private static BundleContext myContext;

    public OpenShiftCommonCoreActivator() {
        super();
        instance = this;
    }

    public static OpenShiftCommonCoreActivator getDefault() {
        return instance;
    }

    public static BundleContext getBundleContext() {
        return myContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        myContext = context;
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

    public static void log(Throwable t) {
        log(null, t);
    }

    public static void log(String message, Throwable t) {
        log(StatusFactory.errorStatus(PLUGIN_ID, message, t));
    }

    public static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

}