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

import java.io.IOException;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_DOMAIN_ID = "domainId";
	public static final String PROPERTY_SSHKEY = "sshKey";

	private String domainId;
	private IDomain domain;
	private UserDelegate user;

	public NewDomainWizardPageModel(UserDelegate user) {
		this.user = user;
	}
	
	public String getDomainId() {
		return this.domainId;
	}

	public void createDomain() throws OpenShiftException, IOException {
		user.createDomain(domainId);
	}
		
	public void setDomainId(String domainId) {
		firePropertyChange(PROPERTY_DOMAIN_ID, this.domainId, this.domainId = domainId);
	}

	public boolean hasDomain() {
		return domain != null;
	}

	public IDomain getDomain() {
		return domain;
	}

	public UserDelegate getUser() {
		return user;
	}
}
