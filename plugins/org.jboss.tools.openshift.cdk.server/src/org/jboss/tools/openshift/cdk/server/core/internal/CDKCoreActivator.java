/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal;

import org.eclipse.core.runtime.Plugin;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListenerManager;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ConfigureDependentFrameworksListener;
import org.osgi.framework.BundleContext;

public class CDKCoreActivator extends Plugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.openshift.cdk.server.core"; //$NON-NLS-1$

	// The shared instance
	private static CDKCoreActivator plugin;
	
	/**
	 * The constructor
	 */
	public CDKCoreActivator() {
	}

	
	private UnitedServerListener configureDependentFrameworksListener;
	
	private UnitedServerListener getConfigureDependentFrameworksListener() {
		if( configureDependentFrameworksListener == null ) {
			configureDependentFrameworksListener = new ConfigureDependentFrameworksListener();
		}
		return configureDependentFrameworksListener;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		UnitedServerListenerManager.getDefault().addListener(getConfigureDependentFrameworksListener());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		UnitedServerListenerManager.getDefault().removeListener(getConfigureDependentFrameworksListener());
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CDKCoreActivator getDefault() {
		return plugin;
	}


}
