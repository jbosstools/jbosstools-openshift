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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.CartridgeNameComparator;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftUserPreferencesProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.IEmbedCartridgesWizardPageModel;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo implements
		IEmbedCartridgesWizardPageModel {

	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROPERTY_APPLICATION_SCALE = "scale";
	public static final String PROPERTY_EXISTING_APPLICATION_NAME = "existingApplicationName";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_EMBEDDED_CARTRIDGES = "embeddedCartridges";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_SELECTED_GEAR_PROFILE = "selectedGearProfile";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_EXISTING_APPLICATIONS_LOADED = "existingApplicationsLoaded";
	public static final String PROPERTY_SCALABLE_APPLICATION = "scalableApplication";
	public static final String PROPERTY_GEAR_PROFILES = "gearProfiles";
	public static final String PROPERTY_DEFAULT_SOURCECODE = "defaultSourcecode";
	public static final String PROPERTY_INITIAL_GITURL = "initialGitUrl";

	private final OpenShiftApplicationWizardModel wizardModel;

	// start with a null value as a marker of non-initialized state (used during
	// first pass validation)
	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<IStandaloneCartridge> cartridges = new ArrayList<IStandaloneCartridge>();
	private List<IGearProfile> gearProfiles = new ArrayList<IGearProfile>();
	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private String existingApplicationName;
	private boolean existingApplicationsLoaded = false;
	private OpenShiftUserPreferencesProvider openShiftUserPreferencesProvider = new OpenShiftUserPreferencesProvider();
	private boolean defaultSourcecode = true;
	
	protected ApplicationConfigurationWizardPageModel(OpenShiftApplicationWizardModel wizardModel)
			throws OpenShiftException {
		this.wizardModel = wizardModel;
		setExistingApplication(wizardModel.getApplication());
	}

	/**
	 * @return the wizardModel
	 */
	public final OpenShiftApplicationWizardModel getWizardModel() {
		return wizardModel;
	}

	public Connection getConnection() {
		return wizardModel.getConnection();
	}

	public List<IApplication> getApplications() throws OpenShiftException, SocketTimeoutException {
		Connection user = getConnection();
		if (user == null || !user.isConnected() || !user.hasDomain()) {
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

	public ApplicationScale getScale() {
		return wizardModel.getApplicationScale();
	}

	public void setScale(ApplicationScale scale) {
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
	public void setExistingApplicationName(String applicationName) throws OpenShiftException {
		firePropertyChange(PROPERTY_EXISTING_APPLICATION_NAME
				, this.existingApplicationName, this.existingApplicationName = applicationName);

		if (!StringUtils.isEmpty(applicationName)
				&& isExistingApplication(applicationName)) {
			doSetExistingApplication(getExistingApplication(applicationName));
		}
	}

	public void loadExistingApplications() throws OpenShiftException, SocketTimeoutException {
		Connection user = getConnection();
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
		Connection connection = getConnection();
		if (connection != null) {
			setCartridges(connection.getStandaloneCartridgeNames());
			refreshSelectedCartridge();
		}
	}

	public void setCartridges(List<IStandaloneCartridge> cartridges) {
		Collections.sort(cartridges, new CartridgeNameComparator());
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
		final String lastSelectedCartridgeName = openShiftUserPreferencesProvider.getLastSelectedCartridgeName();
		final IStandaloneCartridge selectedCartridge = getCartridgeByName(lastSelectedCartridgeName);
		setSelectedCartridge(selectedCartridge);
	}

	public List<IStandaloneCartridge> getCartridges() {
		return cartridges;
	}

	public IStandaloneCartridge getSelectedCartridge() {
		return wizardModel.getApplicationCartridge();
	}

	public void loadGearProfiles() throws OpenShiftException, SocketTimeoutException {
		setGearProfiles(getConnection().getDefaultDomain().getAvailableGearProfiles());
		// refreshSelectedCartridge();
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
		if (gearProfiles == null || name == null) {
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

	public IStandaloneCartridge getCartridgeByName(String name) {
		List<IStandaloneCartridge> cartridges = getCartridges();
		if (cartridges == null || name == null) {
			return null;
		}

		IStandaloneCartridge matchingCartridge = null;
		for (IStandaloneCartridge cartridge : cartridges) {
			if (name.equals(cartridge.getName())) {
				matchingCartridge = cartridge;
				break;
			}
		}

		return matchingCartridge;
	}
	
	/**
	 * forces property change listeners to update their value
	 */
	protected void refreshSelectedCartridge() {
		IStandaloneCartridge selectedCartridge = getSelectedCartridge();
		setSelectedCartridge((IStandaloneCartridge) null);
		setSelectedCartridge(selectedCartridge);
	}

	public void setSelectedCartridge(IStandaloneCartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE
				, wizardModel.getApplicationCartridge()
				, wizardModel.setApplicationCartridge(cartridge));
		if(cartridge != null) {
			openShiftUserPreferencesProvider.setLastSelectedCartridgeName(cartridge.getName());
		}
	}

	protected void setSelectedCartridge(IApplication application) {
		final IStandaloneCartridge applicationCartridge = (application != null) ? application.getCartridge() : null;
		setSelectedCartridge(applicationCartridge);
	}

	public List<IEmbeddableCartridge> loadEmbeddedCartridges() throws OpenShiftException, SocketTimeoutException {
		List<IEmbeddableCartridge> cartridges = getConnection().getEmbeddableCartridges();
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
	public void setExistingApplication(IApplication application) throws OpenShiftException {
		if (application != null) {
			setExistingApplicationName(application.getName());
			// already called within setExistingApplicationName(String) above
			// doSetExistingApplication(application); 
		} else {
			setExistingApplicationName(null);
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
	protected void doSetExistingApplication(IApplication application) throws OpenShiftException {
		if (application != null) {
			setApplicationName(application.getName());
			setSelectedCartridge(application.getCartridge());
			setSelectedEmbeddableCartridges(application.getEmbeddedCartridges());
			setSelectedGearProfile(application.getGearProfile());
			setScale(application.getApplicationScale());
			wizardModel.setApplication(application);
		}
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


	@Override
	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException,SocketTimeoutException {
		getSelectedEmbeddableCartridges().add(cartridge);
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, null, getSelectedEmbeddableCartridges());
	}

	@Override
	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException,SocketTimeoutException {
		getSelectedEmbeddableCartridges().remove(cartridge);
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, null, getSelectedEmbeddableCartridges());
	}

	@Override
	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getSelectedEmbeddableCartridges();
	}

	protected void setSelectedEmbeddableCartridges(List<? extends IEmbeddableCartridge> cartridges) {
		setSelectedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>(cartridges));
	}
	
	public void setSelectedEmbeddableCartridges(Set<IEmbeddableCartridge> selectedEmbeddableCartridges) {
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES,
				wizardModel.getSelectedEmbeddableCartridges(),
				wizardModel.setSelectedEmbeddableCartridges(selectedEmbeddableCartridges));
	}

	public boolean isSelected(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException {
		return getSelectedEmbeddableCartridges().contains(cartridge);
	}

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException {
		// no application yet, no cartridge embedded yet
		return false;
	}
	
	public boolean hasApplication(String applicationName) throws OpenShiftException {
		return getConnection().hasApplication(applicationName);
	}

	@Override
	public IDomain getDomain() throws SocketTimeoutException, OpenShiftException {
		return wizardModel.getConnection().getDefaultDomain();
	}
	
	public void reset() throws OpenShiftException {
		setApplicationName(wizardModel.getApplication());
		setExistingApplication(wizardModel.getApplication());
		setUseExistingApplication(wizardModel.isUseExistingApplication());
		setSelectedEmbeddableCartridges(wizardModel.getSelectedEmbeddableCartridges());
	}
	
	public boolean isDefaultSourcecode() {
		return defaultSourcecode;
	}

	public void setDefaultSourcecode(boolean defaultSourcecode) {
		resetInitialGitUrl();
		firePropertyChange(PROPERTY_DEFAULT_SOURCECODE, this.defaultSourcecode, this.defaultSourcecode = defaultSourcecode);
	}
	
	public String getInitialGitUrl() {
		return wizardModel.getInitialGitUrl();
	}
	
	public void setInitialGitUrl(String initialGitUrl) {
		firePropertyChange(PROPERTY_INITIAL_GITURL, 
				wizardModel.getInitialGitUrl(),
				wizardModel.setInitialGitUrl(initialGitUrl));
	}
	
	public void resetInitialGitUrl() {
		setInitialGitUrl(null);
	}
}
