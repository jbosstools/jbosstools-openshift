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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.utils.PojoEventBridge;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class SelectApplicationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_DOMAINS = "domains";
	public static final String PROPERTY_SELECTED_APPLICATION = "selectedApplication";

	private IApplication selectedApplication;
	private OpenShiftApplicationWizardModel wizardModel;

	public SelectApplicationWizardPageModel(OpenShiftApplicationWizardModel wizardModel) {
		this.selectedApplication = wizardModel.getApplication();
		this.wizardModel = wizardModel;
		new PojoEventBridge()
			.listenTo(IOpenShiftApplicationWizardModel.PROP_DOMAINS, wizardModel).forwardTo(PROPERTY_DOMAINS, this);
	}

	public void refresh() {
		refreshDomains();
		this.selectedApplication = null;
	}
	
	private void refreshDomains() {
		wizardModel.getLegacyConnection().refresh();
		wizardModel.setDomains(loadDomains());
	}

	public List<IDomain> getDomains() throws OpenShiftException {
		return wizardModel.getDomains();
	}

	public IApplication getSelectedApplication() {
		return selectedApplication;
	}

	public void setSelectedApplication(IApplication application) {
		firePropertyChange(PROPERTY_SELECTED_APPLICATION, 
				this.selectedApplication, this.selectedApplication = application);
	}

	public void loadOpenShiftResources() {
		loadDomains();
	}

	protected List<IDomain> loadDomains() {
		List<IDomain> domains = wizardModel.getLegacyConnection().getDomains();
		wizardModel.setDomains(domains);
		return domains;
	}
	
	public ExpressConnection getConnection() {
		return wizardModel.getLegacyConnection();
	}

	public void clearSelectedApplication() {
		setSelectedApplication(null);
	}
}
