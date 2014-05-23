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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferences implements IOpenShiftPreferenceConstants {

	/* Legacy pref location */
	private static final String UI_PLUGIN_ID = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$

	private static final String DEFAULT_TAIL_FILE_OPTIONS = "-f -n 100 */logs/*";

	public static final OpenShiftPreferences INSTANCE = new OpenShiftPreferences();

	private StringsPreferenceValue connectionsPreferenceValue;
	private StringsPreferenceValue legacyConnections;

	/* The following three keys are from the legacy UI plugin pref-store */
	private StringsPreferenceValue UI_connectionsPreferenceValue;
	private StringsPreferenceValue UI_legacyConnections;

	private StringsPreferenceValue tailFileOptionsPreferenceValues;
	private Map<String, String> tailOptionsByUUID = new HashMap<String, String>();

	private StringsPreferenceValue snapshotFilesPreferenceValues;
	private Map<String, Snapshots> snapshotsByUUID = new HashMap<String, Snapshots>();

	private OpenShiftPreferences() {
		this.connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, OpenShiftCoreActivator.PLUGIN_ID);
		this.legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY,
				OpenShiftCoreActivator.PLUGIN_ID);

		this.UI_connectionsPreferenceValue =
				new StringsPreferenceValue('|', CONNECTIONS, UI_PLUGIN_ID);
		this.UI_legacyConnections = new StringsPreferenceValue('|', RHLOGIN_LIST_PREFS_KEY, UI_PLUGIN_ID);

		this.tailFileOptionsPreferenceValues = new StringsPreferenceValue('|', TAIL_FILE_OPTIONS,
				OpenShiftCoreActivator.PLUGIN_ID);
		initTailFileOptions(tailFileOptionsPreferenceValues.get());

		this.snapshotFilesPreferenceValues = new StringsPreferenceValue('|', SNAPSHOT_FILES,
				OpenShiftCoreActivator.PLUGIN_ID);
		initSnapshotFiles(snapshotFilesPreferenceValues.get());
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

	private void initSnapshotFiles(String[] snapshotFiles) {
		if (snapshotFiles == null
				|| snapshotFiles.length == 0) {
			return;
		}
		for (int i = 0; i < snapshotFiles.length - 1; i += 3) {
			String uuid = snapshotFiles[i];
			String fullSnapshotFile = snapshotFiles[i + 1];
			String deploymentSnapshotFile = snapshotFiles[i + 2];
			snapshotsByUUID.put(uuid, new Snapshots(fullSnapshotFile, deploymentSnapshotFile));
		}
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

	public String getTailFileOptions(IApplication application) {
		String tailFileOptions = null;
		if (application != null) {
			tailFileOptions = tailOptionsByUUID.get(application.getUUID());
		}
		if (tailFileOptions == null
				|| tailFileOptions.isEmpty()) {
			tailFileOptions = DEFAULT_TAIL_FILE_OPTIONS;
		}
		return tailFileOptions;
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
			// TODO: remove/cleanup killed applications
			String uuid = entry.getKey();
			String options = entry.getValue();
			uuidsAndOptions.add(uuid);
			uuidsAndOptions.add(options);
		}
		tailFileOptionsPreferenceValues.store(
				uuidsAndOptions.toArray(new String[uuidsAndOptions.size()]));
	}

	public String getFullSnapshot(IApplication application) {
		String fullSnapshot = null;
		Snapshots snapshots = getSnapshots(application);
		if (snapshots != null) {
			fullSnapshot = snapshots.getFullSnapshotFile();
		}
		return fullSnapshot;
	}

	public String getDeploymentSnapshot(IApplication application) {
		String deploymentSnapshot = null;
		Snapshots snapshots = getSnapshots(application);
		if (snapshots != null) {
			deploymentSnapshot = snapshots.getDeploymentSnapshotFile();
		}
		return deploymentSnapshot;
	}

	public Snapshots getSnapshots(IApplication application) {
		if (application == null) {
			return null;
		}
		return snapshotsByUUID.get(application.getUUID());
	}

	public void saveSnapshots(IApplication application, String fullSnapshotFile,
			String deploymentSnapshotFile) {
		if (application != null) {
			snapshotsByUUID.put(application.getUUID(), new Snapshots(fullSnapshotFile, deploymentSnapshotFile));
		}
		saveAllSnapshots();
	}

	public void saveFullSnapshot(IApplication application, String filepath) {
		if (application == null) {
			return;
		}
		
		Snapshots snapshots = getSnapshots(application);
		if (snapshots == null) {
			snapshots = new Snapshots(filepath, null);
		}
		snapshotsByUUID.put(application.getUUID(), snapshots);
		saveAllSnapshots();
	}
	
	public void saveDeploymentSnapshot(IApplication application, String filepath) {
		if (application == null) {
			return;
		}
		
		Snapshots snapshots = getSnapshots(application);
		if (snapshots == null) {
			snapshots = new Snapshots(null, filepath);
		}
		snapshotsByUUID.put(application.getUUID(), snapshots);
		saveAllSnapshots();
	}
	
	private void saveAllSnapshots() {
		List<String> uuidsAndSnapshots = new ArrayList<String>();
		for (Map.Entry<String, Snapshots> entry : snapshotsByUUID.entrySet()) {
			// TODO: remove/cleanup killed applications
			String uuid = entry.getKey();
			Snapshots snapshots = entry.getValue();
			uuidsAndSnapshots.add(uuid);
			uuidsAndSnapshots.add(snapshots.getFullSnapshotFile());
			uuidsAndSnapshots.add(snapshots.getDeploymentSnapshotFile());
		}
		tailFileOptionsPreferenceValues.store(
				uuidsAndSnapshots.toArray(new String[uuidsAndSnapshots.size()]));
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

	public void setClientReadTimeout(int timeout) {
		getPrefs(OpenShiftCoreActivator.PLUGIN_ID).put(CLIENT_READ_TIMEOUT, String.valueOf(timeout));
	}

	public int getClientReadTimeout(int defaultTimeout) {
		String timeout = getPrefs(
				OpenShiftCoreActivator.PLUGIN_ID).get(CLIENT_READ_TIMEOUT, String.valueOf(defaultTimeout));
		return toInteger(timeout);
	}

	public void flush() {
		// TODO: implement
	}

	private int toInteger(String value) {
		if (StringUtils.isEmptyOrNull(value)) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public class Snapshots {

		private String fullSnapshotFile;
		private String deploymentSnapshotFile;

		private Snapshots(String fullSnapshotFile, String deploymentSnapshotFile) {
			this.fullSnapshotFile = fullSnapshotFile;
			this.deploymentSnapshotFile = deploymentSnapshotFile;
		}

		public String getFullSnapshotFile() {
			return fullSnapshotFile;
		}

		public String getDeploymentSnapshotFile() {
			return deploymentSnapshotFile;
		}
	}
}
