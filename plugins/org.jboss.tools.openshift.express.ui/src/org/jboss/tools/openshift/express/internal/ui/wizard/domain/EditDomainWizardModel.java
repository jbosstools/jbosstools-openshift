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

import java.net.SocketTimeoutException;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_DOMAIN_ID = "domainId";

	private String domainId;
	private final IDomain domain;

	private Connection connection;

	public EditDomainWizardModel(Connection connection) {
		this(null, connection);
	}

	public EditDomainWizardModel(IDomain domain) {
		this(domain, null);
	}
	
	protected EditDomainWizardModel(IDomain domain, Connection connection) {
		this.domain = domain;
		this.connection = connection;
		if (domain == null) {
			return;
		}
		setDomainId(domain.getId());
	}

	public String getDomainId() {
		return this.domainId;
	}

	public void setDomainId(String domainId) {
		firePropertyChange(PROPERTY_DOMAIN_ID, 
				this.domainId, this.domainId = domainId);
	}

	public void renameDomain() throws OpenShiftException, SocketTimeoutException {
		if (domain == null) {
			Logger.warn("Attempting to rename missing user domain...");
			return;
		}
		domain.rename(domainId);
	}
	
	public boolean isCurrentDomainId(String domainId) {
		try {
			if (domain == null) {
				return false;
			} 
			return domain.getId().equals(domainId);
		} catch (Exception e) {
			OpenShiftUIActivator.log(e);
			return true;
		}
	}
	
	public IDomain getDomain() {
		return domain;
	}

	public Connection getConnection() {
		return connection;
	}
}
