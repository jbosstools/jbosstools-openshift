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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IDomain;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_NAMESPACE = "namespace";
	public static final String PROPERTY_DOMAIN = "domain";

	private String namespace;
	private IDomain domain;

	public EditDomainWizardPageModel(IUser user) {
		try {
			setDomain(user.getDomain());
		} catch (OpenShiftException e) {
			Logger.error("Fail to retrieve OpenShift domain", e);
		}
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		firePropertyChange(PROPERTY_NAMESPACE, this.namespace, this.namespace = namespace);
	}

	public boolean hasDomain() {
		return domain != null;
	}

	public IDomain getDomain() {
		return domain;
	}

	public void setDomain(IDomain domain) {
		firePropertyChange(PROPERTY_DOMAIN, this.domain, this.domain = domain);
		if (domain != null) {
			setNamespace(domain.getNamespace());
		}
	}

	public void renameDomain() throws OpenShiftException {
		domain.setNamespace(namespace);
	}
}
