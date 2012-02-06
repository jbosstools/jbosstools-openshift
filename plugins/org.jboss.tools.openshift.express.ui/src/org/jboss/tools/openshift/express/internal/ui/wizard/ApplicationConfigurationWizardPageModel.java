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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andr� Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_APPLICATION_NAME_STATUS = "applicationNameStatus";

	private CreateNewApplicationWizardModel wizardModel;

	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<ICartridge> cartridges = new ArrayList<ICartridge>();
	private List<IEmbeddableCartridge> embeddableCartridges = new ArrayList<IEmbeddableCartridge>();
	private String applicationName;
	private IStatus applicationNameStatus;
	private ICartridge selectedCartridge;

	public ApplicationConfigurationWizardPageModel(CreateNewApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}
	
	public IUser getUser() {
//		return wizardModel.getUser();
		return OpenShiftUIActivator.getDefault().getUser();
	}

	public void loadExistingApplications() throws OpenShiftException {
		IUser user = getUser();
		if (user != null) {
			setExistingApplications(user.getApplications());
		}
	}
	
	/**
	 * @return the existingApplications
	 */
	public List<IApplication> getExistingApplications() {
		return existingApplications;
	}

	/**
	 * @param existingApplications the existingApplications to set
	 */
	public void setExistingApplications(List<IApplication> existingApplications) {
		this.existingApplications = existingApplications;
	}

	public void loadCartridges() throws OpenShiftException {
//		setCartridges(wizardModel.getUser().getCartridges());
		setCartridges(OpenShiftUIActivator.getDefault().getUser().getCartridges());
	}

	public void setCartridges(List<ICartridge> cartridges) {
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
	}

	public List<ICartridge> getCartridges() {
		return cartridges;
	}

	public ICartridge getSelectedCartridge() {
		return selectedCartridge;
	}

	public void setSelectedCartridge(ICartridge cartridge) {
		wizardModel.setApplicationCartridge(cartridge);
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE, selectedCartridge, this.selectedCartridge = cartridge);
		validateApplicationName();
	}

	public List<IEmbeddableCartridge> loadEmbeddableCartridges() throws OpenShiftException {
//		List<IEmbeddableCartridge> cartridges = wizardModel.getUser().getEmbeddableCartridges();
		List<IEmbeddableCartridge> cartridges = OpenShiftUIActivator.getDefault().getUser().getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		return cartridges;
	}

	public void setApplicationName(String applicationName) {
		wizardModel.setApplicationName(applicationName);
		firePropertyChange(PROPERTY_APPLICATION_NAME, this.applicationName, this.applicationName = applicationName);
		validateApplicationName();
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public void setApplicationNameStatus(IStatus applicationNameStatus) {
		firePropertyChange(PROPERTY_APPLICATION_NAME_STATUS, this.applicationNameStatus,
				this.applicationNameStatus = applicationNameStatus);
	}

	public IStatus getApplicationNameStatus() {
		return this.applicationNameStatus;
	}

	public IStatus validateApplicationName() {
		IStatus status = Status.OK_STATUS;
		final String applicationName = getApplicationName();
		if (!applicationName.matches("\\S+")) {
			status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"The application name must not contain spaces.");
		}
		for(IApplication application : getExistingApplications()) {
			if(application.getName().equalsIgnoreCase(applicationName)) {
				status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
						"An application with the same name already exists on OpenShift.");
			}
		}
		setApplicationNameStatus(status);
		return status;
	}

	public void setEmbeddableCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddableCartridges,
				this.embeddableCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddableCartridges() {
		return embeddableCartridges;
	}

	public List<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getSelectedEmbeddableCartridges();
	}

	public boolean hasApplication(ICartridge cartridge) {
		try {
//			return wizardModel.getUser().hasApplication(cartridge);
			return getUser().hasApplication(cartridge);
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(OpenShiftUIActivator
					.createErrorStatus("Could not get application by cartridge", e));
			return false;
		}
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException {
		return wizardModel.createApplication(name, ICartridge.JENKINS_14, monitor);
	}

}
