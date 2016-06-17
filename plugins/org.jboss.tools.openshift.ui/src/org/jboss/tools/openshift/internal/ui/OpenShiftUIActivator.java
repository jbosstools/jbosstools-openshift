/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.foundation.ui.plugin.BaseUIPlugin;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.framework.BundleContext;

import com.openshift.restclient.OpenShiftException;

public class OpenShiftUIActivator extends BaseUIPlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.ui"; //$NON-NLS-1$

	private static OpenShiftUIActivator plugin;

	private IPreferenceStore corePreferenceStore;

	public OpenShiftUIActivator() {
	}

	public IPluginLog getLogger() {
		return pluginLogInternal();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		//JBIDE-22612: We need to force OpenShift Core to start so that Connections get loaded in the Explorer
		OpenShiftCoreActivator.getDefault();

		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static OpenShiftUIActivator getDefault() {
		return plugin;
	}

	public static StatusFactory statusFactory() {
		return getDefault().statusFactoryInternal();
	}

	/**
	 * Get an inputstream for a file
	 * 
	 * @param file
	 * @return
	 * @throws OpenShiftException
	 *             if unable to read the file;
	 */
	public InputStream getPluginFile(String file) {
		URL url;
		try {
			url = new URL(plugin.getBundle().getEntry("/"), file);
			return url.openStream();
		} catch (Exception e) {
			getLogger().logError(e);
			throw new OpenShiftException(e, "Exception trying to load plugin file: {0}", file);
		}
	}

	/**
	 * Retrieve the preferencestore
	 * 
	 * @return
	 */
	public IPreferenceStore getCorePreferenceStore() {
		// Create the preference store lazily.
		if (corePreferenceStore == null) {
			this.corePreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
					OpenShiftCoreActivator.PLUGIN_ID);

		}
		return corePreferenceStore;
	}

	public static void log(int status, String message) {
		log(status, message, null);
	}

	public static void log(int status, String message, Throwable e) {
		OpenShiftUIActivator instance = getDefault();
		instance.getLog().log(new Status(status, instance.getId(), message, e));
	}

	private String getId() {
		return getBundle().getSymbolicName();
	}
}
