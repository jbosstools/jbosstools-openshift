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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROPERTY_APPLICATION_SCALE = "applicationScale";
	public static final String PROPERTY_EXISTING_APPLICATION_NAME = "existingApplicationName";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_EMBEDDED_CARTRIDGES = "embeddedCartridges";
	public static final String PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_SELECTED_GEAR_PROFILE = "selectedGearProfile";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_EXISTING_APPLICATIONS_LOADED = "existingApplicationsLoaded";
	public static final String PROPERTY_SCALABLE_APPLICATION = "scalableApplication";
	public static final String PROPERTY_GEAR_PROFILES = "gearProfiles";
	

	private final OpenShiftExpressApplicationWizardModel wizardModel;

	// start with a null value as a marker of non-initialized state (used during
	// first pass validation)
	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<ICartridge> cartridges = new ArrayList<ICartridge>();
	private List<IGearProfile> gearProfiles = new ArrayList<IGearProfile>();
	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private String existingApplicationName;
	private boolean existingApplicationsLoaded = false;;

	public ApplicationConfigurationWizardPageModel(OpenShiftExpressApplicationWizardModel wizardModel)
			throws OpenShiftException, SocketTimeoutException {
		this.wizardModel = wizardModel;
		setExistingApplication(wizardModel.getApplication());
	}

	/**
	 * @return the wizardModel
	 */
	public final OpenShiftExpressApplicationWizardModel getWizardModel() {
		return wizardModel;
	}

	public UserDelegate getUser() {
		return wizardModel.getUser();
	}

	public List<IApplication> getApplications() throws OpenShiftException, SocketTimeoutException {
		UserDelegate user = getUser();
		if (user == null || !user.hasDomain()) {
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
		} catch (SocketTimeoutException e) {
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
	
	public ApplicationScale getApplicationScale() {
		return wizardModel.getApplicationScale();
	}

	public void setApplicationScale(ApplicationScale scale) {
		firePropertyChange(PROPERTY_APPLICATION_SCALE
				, wizardModel.getApplicationScale()
				, wizardModel.setApplicationScale(scale));
	}


	protected void setUseExistingApplication(IApplication application) {
		setUseExistingApplication(application != null);
	}

	public String getExistingApplicationName() {
		return existingApplicationName;
	}

	/**
	 * Sets the existing application in this model by name. If there's an
	 * existing application with the given name, all properties related to an
	 * existing application are also set.
	 * 
	 * @param applicationName
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException 
	 * 
	 * @see #doSetExistingApplication(IApplication)
	 */
	public void setExistingApplicationName(String applicationName) throws OpenShiftException, SocketTimeoutException {
		firePropertyChange(PROPERTY_EXISTING_APPLICATION_NAME
				, this.existingApplicationName, this.existingApplicationName = applicationName);

		if (!StringUtils.isEmpty(applicationName)
				&& isExistingApplication(applicationName)) {
			doSetExistingApplication(getExistingApplication(applicationName));
		}
	}

	public void loadExistingApplications() throws OpenShiftException, SocketTimeoutException {
		UserDelegate user = getUser();
		if (user != null) {
			setExistingApplications(user.getApplications());
			setExistingApplicationsLoaded(true);
		}
	}

	public void setExistingApplicationsLoaded(boolean loaded) {
		firePropertyChange(PROPERTY_EXISTING_APPLICATIONS_LOADED
				, this.existingApplicationsLoaded
				, this.existingApplicationsLoaded = loaded);
	}

	public boolean isExistingApplicationsLoaded() {
		return existingApplicationsLoaded;
	}

	public IApplication getExistingApplication(String applicationName) {
		for (IApplication application : getExistingApplications()) {
			if (application.getName().equalsIgnoreCase(applicationName)) {
				return application;
			}
		}
		return null;
	}

	public boolean isExistingApplication(String applicationName) {
		return getExistingApplication(applicationName) != null;
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

	public List<IApplication> getExistingApplications() {
		return existingApplications;
	}

	public void loadStandaloneCartridges() throws OpenShiftException, SocketTimeoutException {
		setCartridges(getUser().getStandaloneCartridgeNames());
		refreshSelectedCartridge();
	}

	public void setCartridges(List<ICartridge> cartridges) {
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
	}


	public List<ICartridge> getCartridges() {
		return cartridges;
	}
	
	public ICartridge getCartridgeByName(String name) {
		List<ICartridge> cartridges = getCartridges();
		if (cartridges == null) {
			return null;
		}
		
		ICartridge matchingCartridge = null; 
		for (ICartridge cartridge : cartridges) {
			if (name.equals(cartridge.getName())) {
				matchingCartridge = cartridge;
				break;
			}
		}
		
		return matchingCartridge;
	}

	public ICartridge getSelectedCartridge() {
		return wizardModel.getApplicationCartridge();
	}

	public void loadGearProfiles() throws OpenShiftException, SocketTimeoutException {
		setGearProfiles(getUser().getDefaultDomain().getAvailableGearProfiles());
		//refreshSelectedCartridge();
	}
	
	
	public void setGearProfiles(List<IGearProfile> gearProfiles) {
		firePropertyChange(PROPERTY_GEAR_PROFILES, this.gearProfiles, this.gearProfiles = gearProfiles);
	}
	
	public List<IGearProfile> getGearProfiles() {
		return gearProfiles;
	}
	
	public IGearProfile getSelectedGearProfile() {
		return wizardModel.getApplicationGearProfile();
	}

	public void setSelectedGearProfile(IGearProfile gearProfile) {
		firePropertyChange(PROPERTY_SELECTED_GEAR_PROFILE
				, wizardModel.getApplicationGearProfile()
				, wizardModel.setApplicationGearProfile(gearProfile));
	}

	public IGearProfile getGearProfileByName(String name) {
		List<IGearProfile> gearProfiles = getGearProfiles();
		if (gearProfiles == null) {
			return null;
		}
		
		IGearProfile matchingGearProfile = null; 
		for (IGearProfile gearProfile : gearProfiles) {
			if (name.equals(gearProfile.getName())) {
				matchingGearProfile = gearProfile;
				break;
			}
		}
		
		return matchingGearProfile;
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
		final ICartridge applicationCartridge = (application != null) ? application.getCartridge() : null;
		setSelectedCartridge(applicationCartridge);
	}

	public List<IEmbeddableCartridge> loadEmbeddedCartridges() throws OpenShiftException, SocketTimeoutException {
		List<IEmbeddableCartridge> cartridges = getUser().getEmbeddableCartridges();
		setEmbeddedCartridges(cartridges);
		return cartridges;
	}

	/**
	 * Sets the properties in this model that are related to an existing
	 * application. The name of the existing application is set!.
	 * 
	 * @param application
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException 
	 * 
	 * @see #setExistingApplicationName(String)
	 * @see #setApplicationName(IApplication)
	 * @see #setSelectedCartridge(IApplication)
	 * @see #setSelectedEmbeddableCartridges(Set)
	 * @see #wizardModel#setApplication
	 */
	public void setExistingApplication(IApplication application) throws OpenShiftException, SocketTimeoutException {
		if (application != null) {
			setExistingApplicationName(application.getName());
			doSetExistingApplication(application);
		}
	}

	/**
	 * Sets the properties in this model that are related to an existing
	 * application. It does not set the name of the existing application!.
	 * 
	 * @param application
	 * @throws OpenShiftException
	 * @throws SocketTimeoutException 
	 * 
	 * @see #setApplicationName(IApplication)
	 * @see #setSelectedCartridge(IApplication)
	 * @see #setSelectedEmbeddableCartridges(Set)
	 * @see #wizardModel#setApplication
	 */
	protected void doSetExistingApplication(IApplication application) throws OpenShiftException, SocketTimeoutException {
		if (application != null) {
			setApplicationName(application.getName());
			setSelectedCartridge(application.getCartridge());
			setSelectedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>(application.getEmbeddedCartridges()));
			setSelectedGearProfile(application.getGearProfile());
			setApplicationScale(application.getApplicationScale());
			wizardModel.setApplication(application);
		}
	}

	public void resetExistingApplication() throws OpenShiftException, SocketTimeoutException {
		setExistingApplication((IApplication) null);
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

	public void setEmbeddedCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(
				PROPERTY_EMBEDDED_CARTRIDGES, this.embeddedCartridges, this.embeddedCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddedCartridges() {
		return embeddedCartridges;
	}

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getSelectedEmbeddableCartridges();
	}

	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException {
		getSelectedEmbeddableCartridges().add(cartridge);
	}

	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException {
		getSelectedEmbeddableCartridges().remove(cartridge);
	}

	public void setSelectedEmbeddableCartridges(Set<IEmbeddableCartridge> selectedEmbeddableCartridges) {
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES,
				wizardModel.getSelectedEmbeddableCartridges(),
				wizardModel.setSelectedEmbeddableCartridges(selectedEmbeddableCartridges));
	}

	public boolean hasApplicationOfType(ICartridge cartridge) throws SocketTimeoutException, OpenShiftException {
		return getUser().hasApplicationOfType(cartridge);
	}

	public boolean hasApplication(String applicationName) throws SocketTimeoutException, OpenShiftException {
		return getUser().hasApplication(applicationName);
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException {
		return wizardModel.createApplication(name, ICartridge.JENKINS_14, ApplicationScale.NO_SCALE, IGearProfile.SMALL, monitor);
	}

}
