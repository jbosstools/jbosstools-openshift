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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROPERTY_EXISTING_APPLICATION_NAME = "existingApplicationName";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";

	private final OpenShiftExpressApplicationWizardModel wizardModel;

	// start with a null value as a marker of non-initialized state (used during
	// first pass validation)
	private List<IApplication> existingApplications = null;
	private List<ICartridge> cartridges = new ArrayList<ICartridge>();
	private List<IEmbeddableCartridge> embeddableCartridges = new ArrayList<IEmbeddableCartridge>();
	private String existingApplicationName;

	public ApplicationConfigurationWizardPageModel(OpenShiftExpressApplicationWizardModel wizardModel)
			throws OpenShiftException {
		this.wizardModel = wizardModel;
		setExistingApplication(wizardModel.getApplication());
	}

	/**
	 * @return the wizardModel
	 */
	public final OpenShiftExpressApplicationWizardModel getWizardModel() {
		return wizardModel;
	}

	public IUser getUser() {
		return wizardModel.getUser();
	}

	public List<IApplication> getApplications() throws OpenShiftException {
		IUser user = getUser();
		if (user == null) {
			return Collections.emptyList();
		}
		return user.getApplications();
	}

	public String[] getApplicationNames() {
		try {
			List<IApplication> applications = getApplications();
			String[] applicationNames = new String[applications.size()];
			for (int i = 0; i < applications.size(); i++) {
				applicationNames[i] = applications.get(i).getName();
			}
			return applicationNames;
		} catch (OpenShiftException e) {
			Logger.error("Failed to retrieve list of OpenShift applications", e);
			return new String[0];
		}
	}

	public boolean isUseExistingApplication() {
		return wizardModel.isUseExistingApplication();
	}

	public void setUseExistingApplication(boolean useExistingApplication) {
		firePropertyChange(PROPERTY_USE_EXISTING_APPLICATION
				, wizardModel.isUseExistingApplication()
				, wizardModel.setUseExistingApplication(useExistingApplication));
	}

	protected void setUseExistingApplication(IApplication application) {
		setUseExistingApplication(application != null);
	}

	public String getExistingApplicationName() {
		return existingApplicationName;
	}

	public void setExistingApplicationName(String applicationName) {
		firePropertyChange(PROPERTY_EXISTING_APPLICATION_NAME
				, existingApplicationName
				, this.existingApplicationName = applicationName);
	}

	protected void setExistingApplicationName(IApplication application) {
		String applicationName = null;
		if (application != null) {
			applicationName = application.getName();
		}
		setExistingApplicationName(applicationName);
	}

	public void loadExistingApplications() throws OpenShiftException {
		IUser user = getUser();
		if (user != null) {
			setExistingApplications(user.getApplications());
		}
	}

	public List<IApplication> getExistingApplications() {
		return existingApplications;
	}

	public boolean isExistingApplication(String applicationName) {
		for (IApplication application : getExistingApplications()) {
			if (application.getName().equalsIgnoreCase(applicationName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param existingApplications
	 *            the existingApplications to set
	 */
	public void setExistingApplications(List<IApplication> existingApplications) {
		firePropertyChange(PROPERTY_EXISTING_APPLICATIONS
				, this.existingApplications
				, this.existingApplications = existingApplications);
	}

	public void loadCartridges() throws OpenShiftException {
		setCartridges(getUser().getCartridges());
		refreshSelectedCartridge();
	}

	public void setCartridges(List<ICartridge> cartridges) {
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
	}

	public List<ICartridge> getCartridges() {
		return cartridges;
	}

	public ICartridge getSelectedCartridge() {
		return wizardModel.getApplicationCartridge();
	}

	/**
	 * forces property change listeners to update their value
	 */
	protected void refreshSelectedCartridge() {
		ICartridge selectedCartridge = getSelectedCartridge();
		setSelectedCartridge((ICartridge) null);
		setSelectedCartridge(selectedCartridge);
	}

	public void setSelectedCartridge(ICartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE
				, wizardModel.getApplicationCartridge()
				, wizardModel.setApplicationCartridge(cartridge));
	}

	protected void setSelectedCartridge(IApplication application) {
		ICartridge applicationCartridge = null;
		if (application != null) {
			applicationCartridge = application.getCartridge();
		}
		setSelectedCartridge(applicationCartridge);
	}

	public List<IEmbeddableCartridge> loadEmbeddableCartridges() throws OpenShiftException {
		List<IEmbeddableCartridge> cartridges = getUser().getEmbeddableCartridges();
		setEmbeddableCartridges(cartridges);
		return cartridges;
	}

	public void setExistingApplication(IApplication application) throws OpenShiftException {
		// setUseExistingApplication(application);
		setExistingApplicationName(application);
		setApplicationName(application);
		setSelectedCartridge(application);
		setSelectedEmbeddableCartridges(application);
		wizardModel.setApplication(application);
	}

	public void resetExistingApplication() throws OpenShiftException {
		setExistingApplication(null);
	}

	public void setApplicationName(String applicationName) {
		firePropertyChange(PROPERTY_APPLICATION_NAME
				, wizardModel.getApplicationName()
				, wizardModel.setApplicationName(applicationName));
	}

	protected void setApplicationName(IApplication application) {
		String applicationName = null;
		if (application != null) {
			applicationName = application.getName();
		}
		setApplicationName(applicationName);
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	public void setEmbeddableCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDABLE_CARTRIDGES, this.embeddableCartridges, this.embeddableCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddableCartridges() {
		return embeddableCartridges;
	}

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getSelectedEmbeddableCartridges();
	}

	public void setSelectedEmbeddableCartridges(Set<IEmbeddableCartridge> selectedEmbeddableCartridges) {
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES,
				wizardModel.getSelectedEmbeddableCartridges(),
				wizardModel.setSelectedEmbeddableCartridges(selectedEmbeddableCartridges));
	}

	protected void setSelectedEmbeddableCartridges(IApplication application) throws OpenShiftException {
		HashSet<IEmbeddableCartridge> selectedEmbeddableCartridges = new HashSet<IEmbeddableCartridge>();
		if (application != null) {
			selectedEmbeddableCartridges.addAll(application.getEmbeddedCartridges());
		}
		setSelectedEmbeddableCartridges(selectedEmbeddableCartridges);
	}

	public boolean hasApplication(ICartridge cartridge) {
		try {
			return getUser().hasApplication(cartridge);
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(
					OpenShiftUIActivator.createErrorStatus("Could not get application by cartridge", e));
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
