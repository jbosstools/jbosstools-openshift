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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
@Deprecated
public class ApplicationSelectionWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROPERTY_SELECTED_APPLICATION = "selectedApplication";

	private boolean useExistingApplication = false;
	private IApplication selectedApplication;
	private IOpenShiftWizardModel wizardModel;

	public ApplicationSelectionWizardPageModel(IOpenShiftWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		this.selectedApplication = wizardModel.getApplication();
		this.useExistingApplication = wizardModel.isExistingApplication();
	}

	public List<IApplication> getApplications() throws OpenShiftException {
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

	public IUser getUser() {
//		return OpenShiftUIActivator.getDefault().getUser();
		return wizardModel.getUser();
	}

	public boolean getUseExistingApplication() {
		return this.useExistingApplication;
	}
	
	public void setUseExistingApplication(boolean useExistingApplication) {
		wizardModel.setUseExistingApplication(useExistingApplication);
		firePropertyChange(PROPERTY_USE_EXISTING_APPLICATION, this.useExistingApplication, this.useExistingApplication = useExistingApplication);
		
	}
	public IStatus getExistingApplicationValidity() {
		// TODO Auto-generated method stub
		return Status.OK_STATUS;
	}

	public boolean hasApplication(String name) {
		// TODO Auto-generated method stub
		return false;
	}

}
