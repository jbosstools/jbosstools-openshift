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

import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.IDomain;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * 
 */
public class ApplicationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_NAMESPACE = "namespace";
	public static final String PROPERTY_DOMAIN = "domain";
	public static final String PROPERTY_SELECTED_APPLICATION = "selectedApplication";

	private String namespace;
	private IDomain domain;
	private IApplication selectedApplication;
	private ImportProjectWizardModel wizardModel;

	public ApplicationWizardPageModel(ImportProjectWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	public String getNamespace() {
		return this.namespace;
	}

	private void setNamespace(IDomain domain) {
		if (domain != null) {
			setNamespace(domain.getNamespace());
		} else {
			setNamespace((String) null);
		}
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
		setNamespace(domain);
	}

	public void renameDomain() throws OpenShiftException {
		getDomain().setNamespace(namespace);
	}

	public void loadDomain() throws OpenShiftException {
		setDomain(getUser().getDomain());
	}

	public List<IApplication> loadApplications() throws OpenShiftException {
		IUser user = getUser();
		if (user == null) {
			return Collections.emptyList();
		}
		return user.getApplications();
	}

	public IApplication getSelectedApplication() {
		return selectedApplication;
	}

	public void setSelectedApplication(IApplication application) {
		wizardModel.setApplication(application);
		firePropertyChange(PROPERTY_SELECTED_APPLICATION, this.selectedApplication, this.selectedApplication = application);
	}

	public void destroyCurrentApplication() throws OpenShiftException {
		if (selectedApplication == null) {
			return;
		}

		selectedApplication.destroy();
	}
	
	public IUser getUser() {
		return wizardModel.getUser();
	}

}
