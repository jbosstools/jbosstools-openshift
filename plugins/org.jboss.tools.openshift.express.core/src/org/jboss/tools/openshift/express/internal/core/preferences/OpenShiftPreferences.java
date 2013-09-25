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
package org.jboss.tools.openshift.express.internal.core.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferences implements IOpenShiftPreferenceConstants {

	/* Legacy pref location */
	private static final String UI_PLUGIN_ID = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$
	
	public static final OpenShiftPreferences INSTANCE = new OpenShiftPreferences();

	private StringsPreferenceValue connectionsPreferenceValue;
	private StringsPreferenceValue legacyConnections;

	/* The following three keys are from the legacy UI plugin pref-store */
	private StringsPreferenceValue UI_connectionsPreferenceValue;
	private StringsPreferenceValue UI_legacyConnections;

	private OpenShiftPreferences() {
		this.connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, OpenShiftCoreActivator.PLUGIN_ID);
		this.legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY, OpenShiftCoreActivator.PLUGIN_ID);
	
		this.UI_connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, UI_PLUGIN_ID);
		this.UI_legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY, UI_PLUGIN_ID);

	}

	private IEclipsePreferences getPrefs(String id) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(id);
		return prefs;
	}

	public String getDefaultHost() {
		String ret = getPrefs(OpenShiftCoreActivator.PLUGIN_ID).get(DEFAULT_HOST, null);
		return ret == null ? getPrefs(UI_PLUGIN_ID).get(DEFAULT_HOST, null) : ret;
	}
	
	public void setDefaultHost(String host) {
		getPrefs(OpenShiftCoreActivator.PLUGIN_ID).put(DEFAULT_HOST, host);
	}

	public String getLastUsername() {
		String ret = getPrefs(OpenShiftCoreActivator.PLUGIN_ID).get(LAST_USERNAME, null);
		return ret == null ? getPrefs(UI_PLUGIN_ID).get(LAST_USERNAME, null) : ret;
	}

	public void saveLastUsername(String username) {
		getPrefs(OpenShiftCoreActivator.PLUGIN_ID).put(LAST_USERNAME, username);
	}

	public String[] getConnections() {
		String[] ret = connectionsPreferenceValue.get();
		return ret == null ? UI_connectionsPreferenceValue.get() : ret;
	}

	public void saveConnections(String[] connections) {
		connectionsPreferenceValue.store(connections);
	}

	public String[] getLegacyConnections() {
		String[] ret = legacyConnections.get();
		return ret == null ? UI_legacyConnections.get() : ret;
	}

	public void saveLegacyConnections(String[] connections) {
		legacyConnections.store(connections);
	}

	public void flush() {
		// TODO: implement
	}

}
