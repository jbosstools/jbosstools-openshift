/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.core.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.openshift.io.core.OSIOCoreConstants;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.jboss.tools.openshift.io.core.model.IAccountModel;
import org.jboss.tools.openshift.io.core.model.IAccountModelListener;
import org.jboss.tools.openshift.io.internal.core.OpenShiftIOCoreActivator;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class AccountModel implements IAccountModel {

	private List<ICluster> clusters = new ArrayList<>();
	
	private List<String> removed = new ArrayList<>();
	
	private List<IAccountModelListener> listeners = new ArrayList<>();

	enum Event {
		ACCOUNT_ADDED,
		ACCOUNT_REMOVED,
		CLUSTER_ADDED,
		CLUSTER_REMOVED
	}
	
	public AccountModel() {
		loadModel();
	}
	
	private void loadModel() {
		ISecurePreferences secureAccountRoot = getSecureAccountsPreferences();
		Preferences accountsRoot = getAccountsPreferences();
		try {
			String[] ids = accountsRoot.childrenNames();
			for(String id: ids) {
				Cluster cluster = new Cluster(this, id);
				Preferences clusterNode = accountsRoot.node(id);
				ISecurePreferences secureClusterNode = secureAccountRoot.node(id);
				try {
					cluster.load(clusterNode, secureClusterNode);
					clusters.add(cluster);
				} catch (StorageException e) {
					OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
				}
			}
		} catch (BackingStoreException e) {
			OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
		}
		if (clusters.isEmpty()) {
			addCluster(createCluster("OpenShift.io"));
		}
	}
	
	void fireEvent(Event event, IAccount account) {
		listeners.stream().forEach(listener -> {
			switch (event) {
			case ACCOUNT_ADDED:
				listener.accountAdded(this, account);
				break;
			case ACCOUNT_REMOVED:
				listener.accountRemoved(this,  account);
				break;
			}
		});
	}
	
	void fireEvent(Event event, ICluster cluster) {
		listeners.stream().forEach(listener -> {
			switch (event) {
			case CLUSTER_ADDED:
				listener.clusterAdded(this, cluster);
				break;
			case CLUSTER_REMOVED:
				listener.clusterRemoved(this,  cluster);
				break;
			}
		});
	}
	
	@Override
	public ICluster createCluster(String id) {
		return new Cluster(this, id);
	}

	@Override
	public void addCluster(ICluster cluster) {
		clusters.add(cluster);
		fireEvent(Event.CLUSTER_ADDED, cluster);
	}

	@Override
	public List<ICluster> getClusters() {
		return clusters;
	}

	@Override
	public void removeCluster(ICluster cluster) {
		clusters.remove(cluster);
		fireEvent(Event.CLUSTER_REMOVED, cluster);
		removed.add(cluster.getId());
	}

	@Override
	public void save() {
		clusters.stream().forEach(ICluster::save);
		Preferences accountRoot = getAccountsPreferences();
		ISecurePreferences accountSecureRoot = getSecureAccountsPreferences();
		removed.stream().forEach(id -> {
			try {
				accountRoot.node(id).removeNode();
				accountSecureRoot.node(id).removeNode();
			} catch (BackingStoreException e) {
				OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
			}
		});
		removed.clear();
	}

	@Override
	public void addListener(IAccountModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(IAccountModelListener listener) {
		listeners.remove(listener);
	}

	static ISecurePreferences getSecureAccountsPreferences() {
		ISecurePreferences secureRoot = SecurePreferencesFactory.getDefault();
		return secureRoot.node(OpenShiftIOCoreActivator.PLUGIN_ID).node(OSIOCoreConstants.ACCOUNT_BASE_KEY);
	}

	static Preferences getAccountsPreferences() {
		IEclipsePreferences root = InstanceScope.INSTANCE.getNode(OpenShiftIOCoreActivator.PLUGIN_ID);
		return root.node(OSIOCoreConstants.ACCOUNT_BASE_KEY);
	}
}
