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
import org.jboss.tools.openshift.core.ConnectionType;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

import com.openshift.client.IDomain;

/**
 * @author André Dietisheim
 */
public class ManageDomainsWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_DOMAIN = "selectedDomain";
	public static final String PROPERTY_DOMAINS = "domains";

	private Connection connection;
	private IDomain selectedDomain;
	private List<IDomain> domains;
	private IConnectionsModelListener connectionChangeListener;

	public ManageDomainsWizardPageModel(IDomain domain, Connection connection) {
		this(connection);
		setSelectedDomain(domain);
	}

	public ManageDomainsWizardPageModel(Connection connection) {
		this.connection = connection;
		this.connectionChangeListener = onConnectionsChanged();
		ConnectionsModelSingleton.getInstance().addListener(connectionChangeListener );
	}

	private IConnectionsModelListener onConnectionsChanged() {
		return new IConnectionsModelListener() {
			
			@Override
			public void connectionRemoved(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				if(ConnectionType.Legacy == type){
					ManageDomainsWizardPageModel.this.connection = null;
					loadDomains();
				}
			}
			
			@Override
			public void connectionChanged(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				if(ConnectionType.Legacy == type){
					setDomains(Collections.<IDomain>emptyList()); // Workaround: force list update
					loadDomains();
				}
			}
			
			@Override
			public void connectionAdded(org.jboss.tools.openshift.core.Connection connection, ConnectionType type) {
				if(ConnectionType.Legacy == type){
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

	public Connection getConnection() {
		return connection;
	}
	
	public void dispose() {
		ConnectionsModelSingleton.getInstance().removeListener(connectionChangeListener);
	}
}
