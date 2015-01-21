/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import java.util.List;

import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IDomain;

/**
 * @author André Dietisheim
 */
public class ManageDomainsWizard extends AbstractOpenShiftWizard<ManageDomainsWizardPageModel> {

	private String description;

	public ManageDomainsWizard(String title, String description, ExpressConnection connection) {
		this(title, description, null, connection);
	}

	public ManageDomainsWizard(String title, String description, IDomain domain, ExpressConnection connection) {
		super(title, new ManageDomainsWizardPageModel(domain, connection));
		this.description = description;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	@Override
	public void addPages() {
		addPage(new ManageDomainsWizardPage(getWindowTitle(), description, getModel(), this));
	}
	
	public List<IDomain> getDomains() {
		return getModel().getDomains();
	}

	public IDomain getDomain() {
		return getModel().getSelectedDomain();
	}
}
