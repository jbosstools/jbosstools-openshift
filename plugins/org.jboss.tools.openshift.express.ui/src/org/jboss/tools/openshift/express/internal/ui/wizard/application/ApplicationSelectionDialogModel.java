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

import java.util.Collections;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationSelectionDialogModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_APPLICATION = "selectedApplication";

	private IApplication selectedApplication;

	private IOpenShiftWizardModel wizardModel;

	public ApplicationSelectionDialogModel(IOpenShiftWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		this.selectedApplication = wizardModel.getApplication();
	}

	public void refresh() {
		IDomain domain = getDomain();
		try {
			if (domain == null) {
				return;
			}
			domain.refresh();
		} catch (OpenShiftException e) {
			Logger.error(NLS.bind("Could not refresh domain {0}", domain.getId()), e);
		}
	}
	
	public List<IApplication> getApplications() {
		try {
			IDomain domain = getDomain();
			if (domain != null) {
				return domain.getApplications();
			}
		} catch (OpenShiftException e) {
			Logger.error("Failed to retrieve applications", e);
		}
		return Collections.emptyList();
	}

	public IApplication getSelectedApplication() {
		return selectedApplication;
	}

	public void setSelectedApplication(IApplication application) {
		firePropertyChange(PROPERTY_SELECTED_APPLICATION, 
				this.selectedApplication, this.selectedApplication = application);
	}

	protected IDomain getDomain() {
		return wizardModel.getDomain();
	}
}
