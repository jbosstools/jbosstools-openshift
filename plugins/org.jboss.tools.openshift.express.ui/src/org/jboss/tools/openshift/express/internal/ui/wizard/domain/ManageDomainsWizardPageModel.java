/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

import com.openshift.client.IDomain;

/**
 * @author André Dietisheim
 */
public class ManageDomainsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_DOMAIN = "selectedDomain";
	public static final String PROPERTY_DOMAINS = "domains";

	private ExpressConnection connection;
	private IDomain selectedDomain;
	private List<IDomain> domains;
	private IConnectionsRegistryListener connectionChangeListener;

	public ManageDomainsWizardPageModel(IDomain domain, ExpressConnection connection) {
		this(connection);
		setSelectedDomain(domain);
	}

	public ManageDomainsWizardPageModel(ExpressConnection connection) {
		this.connection = connection;
		this.connectionChangeListener = onConnectionsChanged();
		ConnectionsRegistrySingleton.getInstance().addListener(connectionChangeListener );
	}

	private IConnectionsRegistryListener onConnectionsChanged() {
		return new IConnectionsRegistryListener() {
			
			@Override
			public void connectionRemoved(IConnection connection) {
				if(ConnectionType.Legacy == connection.getType()){
					ManageDomainsWizardPageModel.this.connection = null;
					loadDomains();
				}
			}
			
			@Override
			public void connectionChanged(IConnection connection) {

				if(ConnectionType.Legacy == connection.getType()){
					setDomains(Collections.<IDomain>emptyList()); // Workaround: force list update
					loadDomains();
				}
			}
			
			@Override
			public void connectionAdded(IConnection connection) {
				if(ConnectionType.Legacy == connection.getType()){
					loadDomains();
				}
			}
		};
	}

	public void loadDomains() {
		if (connection == null) {
			setDomains(Collections.<IDomain>emptyList());
		} else {
			setDomains(connection.getDomains());
		}
	}

	public void setDomains(List<IDomain> domains) {
		firePropertyChange(PROPERTY_DOMAINS, null, this.domains = domains);
	}

	public List<IDomain> getDomains() {
		return domains;
	}

	public void refresh() {
		connection.refresh();
		loadDomains();
	}

	public void setSelectedDomain(IDomain domain) {
		firePropertyChange(PROPERTY_SELECTED_DOMAIN, this.selectedDomain, this.selectedDomain = domain);
	}

	public IDomain getSelectedDomain() {
		return selectedDomain;
	}

	public ExpressConnection getConnection() {
		return connection;
	}
	
	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(connectionChangeListener);
	}
}
