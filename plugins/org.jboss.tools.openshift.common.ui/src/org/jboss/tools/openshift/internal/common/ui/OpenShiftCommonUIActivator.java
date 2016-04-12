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
package org.jboss.tools.openshift.internal.common.ui;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.foundation.ui.plugin.BaseUIPlugin;
import org.osgi.framework.BundleContext;

public class OpenShiftCommonUIActivator extends BaseUIPlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.ui"; //$NON-NLS-1$

	private static OpenShiftCommonUIActivator plugin;
	
	public OpenShiftCommonUIActivator() {
	}
	
	public IPluginLog getLogger(){
		return pluginLogInternal();
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static OpenShiftCommonUIActivator getDefault() {
		return plugin;
	}

	public static void log(Throwable t) {
		log(null, t);
	}

	public static void log(String message, Throwable t) {
		log(StatusFactory.errorStatus(PLUGIN_ID, message, t));
	}
	
	public static void log(IStatus status) {
		plugin.getLog().log(status);
	}

}
