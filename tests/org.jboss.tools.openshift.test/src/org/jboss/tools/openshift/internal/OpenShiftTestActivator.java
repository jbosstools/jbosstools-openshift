/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal;

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftTestActivator extends BaseCorePlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.test"; //$NON-NLS-1$
	
	private static OpenShiftTestActivator instance;
	private static BundleContext context;
	public OpenShiftTestActivator() {
		super();
		instance = this;
	}

	public static OpenShiftTestActivator getDefault() {
	    return instance;
	}

	public static BundleContext getBundleContext() {
	    return context;
	}

	@Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        OpenShiftTestActivator.context = context;
	}

    @Override
	public void stop(BundleContext context) throws Exception {
    	super.stop(context);
    	OpenShiftTestActivator.context = null;
	}

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
