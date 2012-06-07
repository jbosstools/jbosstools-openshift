/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * A small utility class that accesses the preferences to keep the last
 * cartridge that a user selected to create an OpenShift Application.
 * 
 * @author Xavier Coulon
 * 
 */
public class OpenShiftUserPreferencesProvider {

	private static final String LAST_SELECTED_CARTRIDGE_KEY = OpenShiftUIActivator.PLUGIN_ID + ".lastSelectedCartridge";

	/**
	 * @return the last selected value, or null if that preference does not exist yet.
	 */
	public String getLastSelectedCartridgeName() {
		// Find the last-selected one
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(OpenShiftUIActivator.PLUGIN_ID);
		return prefs.get(LAST_SELECTED_CARTRIDGE_KEY, null);
	}

	/**
	 * Stores the given cartridge name in the preferences.
	 * @param name
	 */
	public void setLastSelectedCartridgeName(String name) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(OpenShiftUIActivator.PLUGIN_ID);
		prefs.put(LAST_SELECTED_CARTRIDGE_KEY, name);
	}

}
