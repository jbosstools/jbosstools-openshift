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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.common.ui.preferencevalue.StringPreferenceValue;
import org.jboss.tools.common.ui.preferencevalue.StringsPreferenceValue;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferences implements IOpenShiftPreferenceConstants {

	public static final OpenShiftPreferences INSTANCE = new OpenShiftPreferences();

	private StringsPreferenceValue connectionsPreferenceValue;
	private StringsPreferenceValue legacyConnections;
	private StringPreferenceValue lastUsernamePreferenceValue;
	private StringsPreferenceValue tailFileOptionsPreferenceValues;
	private Map<String, String> tailOptionsByUUID = new HashMap<String, String>();
	
	private OpenShiftPreferences() {
		this.connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, OpenShiftUIActivator.PLUGIN_ID);
		this.legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY, OpenShiftUIActivator.PLUGIN_ID);
		this.lastUsernamePreferenceValue =
				new StringPreferenceValue(LAST_USERNAME, OpenShiftUIActivator.PLUGIN_ID);
		this.tailFileOptionsPreferenceValues = new StringsPreferenceValue('|', TAIL_FILE_OPTIONS, OpenShiftUIActivator.PLUGIN_ID);
		initTailFileOptions(tailFileOptionsPreferenceValues.get());
	}

	private void initTailFileOptions(String[] options) {
		if (options == null
				|| options.length == 0) {
			return;
		}
		for (int i = 0; i < options.length - 1; i += 2) {
			String uuid = options[i];
			String tailOptions = options[i + 1];
			tailOptionsByUUID.put(uuid, tailOptions);
		}
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

	public String getTailFileOptions(IApplication application) {
		if (application == null) {
			return null;
		}
		return tailOptionsByUUID.get(application.getUUID());
	}

	public void saveTailFileOptions(IApplication application, String tailFileOptions) {
		if (application != null) {
			tailOptionsByUUID.put(application.getUUID(), tailFileOptions);
		}
		saveAllTailOptions();
	}

	private void saveAllTailOptions() {
		List<String> uuidsAndOptions = new ArrayList<String>();
		for (Map.Entry<String, String> entry : tailOptionsByUUID.entrySet()) {
			String uuid = entry.getKey();
			String options = entry.getValue();
			uuidsAndOptions.add(uuid);
			uuidsAndOptions.add(options);
		}
		tailFileOptionsPreferenceValues.store(
				uuidsAndOptions.toArray(new String[uuidsAndOptions.size()]));
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
