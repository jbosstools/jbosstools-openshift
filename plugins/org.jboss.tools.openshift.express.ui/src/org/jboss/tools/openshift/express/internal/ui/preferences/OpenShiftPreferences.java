/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.common.ui.preferencevalue.StringsPreferenceValue;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferences implements IOpenShiftPreferenceConstants {

	public static final OpenShiftPreferences INSTANCE = new OpenShiftPreferences();

	private StringsPreferenceValue connectionsPreferenceValue;
	private StringsPreferenceValue legacyConnections;
	private StringPreferenceValue lastUsernamePreferenceValue;

	private OpenShiftPreferences() {
		this.connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, OpenShiftUIActivator.PLUGIN_ID);
		this.legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		this.lastUsernamePreferenceValue =
				new StringPreferenceValue(LAST_USERNAME, OpenShiftUIActivator.PLUGIN_ID);
	}

	public IPreferenceStore getPreferencesStore() {
		return OpenShiftUIActivator.getDefault().getPreferenceStore();
	}

	public String getDefaultHost() {
		return getPreferencesStore().getString(DEFAULT_HOST);
	}
	
	public void setDefaultHost(String host) {
		getPreferencesStore().setValue(DEFAULT_HOST, host);
	}

	public String getLastUsername() {
		return lastUsernamePreferenceValue.get();
	}

	public void saveLastUsername(String username) {
		lastUsernamePreferenceValue.store(username);
	}

	public String[] getConnections() {
		return connectionsPreferenceValue.get();
	}

	public void saveConnections(String[] connections) {
		connectionsPreferenceValue.store(connections);
	}

	public String[] getLegacyConnections() {
		return legacyConnections.get();
	}

	public void saveLegacyConnections(String[] connections) {
		legacyConnections.store(connections);
	}

	public void flush() {
		// TODO: implement
	}

}
