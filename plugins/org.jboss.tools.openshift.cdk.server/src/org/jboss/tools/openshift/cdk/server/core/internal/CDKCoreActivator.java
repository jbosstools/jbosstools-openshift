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

import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListenerManager;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.foundation.ui.plugin.BaseUIPlugin;
import org.jboss.tools.foundation.ui.plugin.BaseUISharedImages;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ConfigureDependentFrameworksListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class CDKCoreActivator extends BaseUIPlugin {
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
	

	/**
	 * Create your shared images instance. Clients are expected to override this
	 */
	protected BaseUISharedImages createSharedImages() {
		return new CDKSharedImages(getBundle());
	}

	public static final String CDK_WIZBAN = "icons/cdk_box_130x65.png";
	private static class CDKSharedImages extends BaseUISharedImages {
		public CDKSharedImages(Bundle pluginBundle) {
			super(pluginBundle);
			addImage(CDK_WIZBAN, CDK_WIZBAN);
		}
	}
}
