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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.net.SocketTimeoutException;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_DOMAIN_ID = "domainId";

	private String domainId;

	private final UserDelegate user;

	public EditDomainWizardPageModel(UserDelegate user) {
		this.user = user;
		try {
			if (user.getDefaultDomain() != null) {
				setDomainId(user.getDefaultDomain().getId());
			}
		} catch (OpenShiftException e) {
			Logger.error("Fail to retrieve OpenShift domain", e);
		} catch (SocketTimeoutException e) {
			Logger.error("Fail to retrieve OpenShift domain", e);
		}
	}

	public String getDomainId() {
		return this.domainId;
	}

	public void setDomainId(String namespace) {
		firePropertyChange(PROPERTY_DOMAIN_ID, this.domainId,
				this.domainId = namespace);
	}

	public void renameDomain() throws OpenShiftException, SocketTimeoutException {
		if (user.getDefaultDomain() != null) {
			user.getDefaultDomain().rename(domainId);
		} else {
			Logger.warn("Attempting to rename missing user domain...");
		}
	}
	
	public boolean isCurrentDomainId(String domainId) {
		IDomain domain;
		try {
			domain = user.getDefaultDomain();
			if (domain == null) {
				return false;
			} 
			return domain.getId().equals(domainId);
		} catch (Exception e) {
			OpenShiftUIActivator.log(e);
			return true;
		}
	}
}
