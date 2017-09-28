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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.openshift.io.core.OSIOCoreConstants;
import org.jboss.tools.openshift.io.core.OSIOUtils;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.jboss.tools.openshift.io.internal.core.OpenShiftIOCoreActivator;
import org.jboss.tools.openshift.io.internal.core.model.AccountModel.Event;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class Cluster implements ICluster {

	private static final String ENDPOINT_URL_KEY = "endpointURL";

	private String id;
	
	private String endpointURL = OSIOCoreConstants.OSIO_ENDPOINT;
	
	private String landingURL;
	
	private List<IAccount> identities = new ArrayList<>();
	
	private List<String> removed = new ArrayList<>();

	private AccountModel model;
	
	public Cluster(AccountModel model, String id) {
		this.model = model;
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getEndpointURL() {
		return endpointURL;
	}

	@Override
	public void setEndpointURL(String endpointURL) {
		this.endpointURL = endpointURL;
	}

	@Override
	public String getLoginURL() {
		return getEndpointURL() +  OSIOCoreConstants.LOGIN_SUFFIX + getLandingURL();
	}

	@Override
	public String getLandingURL() {
		if (null == landingURL) {
			landingURL = OSIOUtils.computeLandingURL(getEndpointURL(), OSIOCoreConstants.DEVSTUDIO_OSIO_LANDING_PAGE_SUFFIX);
		}
		return landingURL;
	}

	@Override
	public String getRefreshURL() {
		return getEndpointURL() + OSIOCoreConstants.REFRESH_SUFFIX;
	}

	@Override
	public void addAccount(IAccount account) {
		identities.add(account);
		model.fireEvent(Event.ACCOUNT_ADDED, account);
	}

	@Override
	public List<IAccount> getAccounts() {
		return identities;
	}

	@Override
	public void removeAccount(IAccount account) {
		identities.remove(account);
		removed.add(account.getId());
		model.fireEvent(Event.ACCOUNT_REMOVED, account);
	}

	@Override
	public IAccount createAccount(String id) {
		return new Account(id, this);
	}

	@Override
	public void save() {
			try {
				Preferences accountsNode = AccountModel.getAccountsPreferences();
				Preferences clusterNode = accountsNode.node(getId());
				ISecurePreferences clusterSecureNode = AccountModel.getSecureAccountsPreferences().node(getId());
				clusterNode.put(ENDPOINT_URL_KEY, getEndpointURL());
				removed.stream().forEach(id -> {
					try {
						clusterNode.node(id).removeNode();
						clusterSecureNode.node(id).removeNode();
					} catch (BackingStoreException e) {
						OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
					}
				});
				removed.clear();

				clusterNode.flush();
				clusterSecureNode.flush();
			} catch (BackingStoreException | IOException e) {
				OpenShiftIOCoreActivator.logError("Error saving cluster in storage", e);
			}
	}

	public void load(Preferences clusterNode, ISecurePreferences secureClusterNode) throws StorageException {
		clusterNode.get(ENDPOINT_URL_KEY, OSIOCoreConstants.OSIO_ENDPOINT);
		try {
			String[] ids = clusterNode.childrenNames();
			for(String id: ids) {
				Account account = new Account(id, this);
				Preferences accountNode = clusterNode.node(id);
				ISecurePreferences secureAccountNode = secureClusterNode.node(id);
				try {
					account.load(accountNode, secureAccountNode);
					identities.add(account);
				} catch (StorageException e) {
					OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
				}
			}
		} catch (BackingStoreException e) {
			OpenShiftIOCoreActivator.logError(e.getLocalizedMessage(), e);
		}

	}
}
