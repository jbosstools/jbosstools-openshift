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
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationSelectionDialogModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED_APPLICATION = "selectedApplication";

	private IApplication selectedApplication;

	private IOpenShiftExpressWizardModel wizardModel;

	public ApplicationSelectionDialogModel(IOpenShiftExpressWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		this.selectedApplication = wizardModel.getApplication();
	}

	public void refresh() {
		try {
			getUser().refresh();
		} catch (OpenShiftException e) {
			Logger.error("Failed to refresh User's account", e);
		}
	}
	
	public List<IApplication> getApplications() {
		try {
			IUser user = getUser();
			if (user == null) {
				return Collections.emptyList();
			}
			return user.getApplications();
		} catch (OpenShiftException e) {
			Logger.error("Failed to retrieve User's applications", e);
		}
		return Collections.emptyList();
	}

	public IApplication getSelectedApplication() {
		return selectedApplication;
	}

	public void setSelectedApplication(IApplication application) {
		firePropertyChange(PROPERTY_SELECTED_APPLICATION, this.selectedApplication,
				this.selectedApplication = application);
	}

	public IUser getUser() {
//		return OpenShiftUIActivator.getDefault().getUser();
		return wizardModel.getUser();
	}

}
