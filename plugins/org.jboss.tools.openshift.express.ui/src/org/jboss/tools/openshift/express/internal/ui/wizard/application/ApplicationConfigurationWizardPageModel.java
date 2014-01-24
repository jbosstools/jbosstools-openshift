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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.CartridgeNameComparator;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftUserPreferencesProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.IEmbedCartridgesWizardPageModel;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @author Martes G Wigglesworth
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo 
	implements IEmbedCartridgesWizardPageModel {

	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_APPLICATION_SCALE = "scale";
	public static final String PROPERTY_CARTRIDGES = "cartridges";
	public static final String PROPERTY_DEFAULT_SOURCECODE = "defaultSourcecode";
	public static final String PROPERTY_DOMAIN = "domain";
	public static final String PROPERTY_DOMAINS = "domains";
	public static final String PROPERTY_EMBEDDED_CARTRIDGES = "embeddedCartridges";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES_SUPPORTED = "environmentVariablesSupported";
	public static final String PROPERTY_EXISTING_APPLICATION_NAME = "existingApplicationName";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_GEAR_PROFILES = "gearProfiles";
	public static final String PROPERTY_INITIAL_GITURL = "initialGitUrl";
	public static final String PROPERTY_RESOURCES_LOADED = "resourcesLoaded";
	public static final String PROPERTY_SCALABLE_APPLICATION = "scalableApplication";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_SELECTED_GEAR_PROFILE = "selectedGearProfile";
	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";

	private final OpenShiftApplicationWizardModel wizardModel;

	// start with a null value as a marker of non-initialized state (used during
	// first pass validation)
	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<IStandaloneCartridge> cartridges = new ArrayList<IStandaloneCartridge>();
	private List<IGearProfile> gearProfiles = new ArrayList<IGearProfile>();
	private List<IEmbeddableCartridge> embeddedCartridges = new ArrayList<IEmbeddableCartridge>();
	private String existingApplicationName;
	private boolean resourcesLoaded = false;
	private OpenShiftUserPreferencesProvider openShiftUserPreferencesProvider = new OpenShiftUserPreferencesProvider();
	private boolean defaultSourcecode = true;
	
	protected ApplicationConfigurationWizardPageModel(OpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setExistingApplication(wizardModel.getApplication());
	}

	public Connection getConnection() {
		return wizardModel.getConnection();
	}

	public List<IApplication> getApplications() throws OpenShiftException {
		IDomain domain = wizardModel.getDomain();
		if (domain == null) {
			return Collections.emptyList();
		}
		return domain.getApplications();
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
	
	private boolean isValid(Connection connection) {
		return connection != null
				&& connection.isConnected();
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

	public String getExistingApplicationName() {
		return existingApplicationName;
	}

	/**
	 * Sets the existing application in this model for the given application name. If there's an
	 * existing application with the given name, all properties related to an
	 * existing application are also set.
	 * 
	 * @param applicationName
	 * @throws OpenShiftException
	 * 
	 * @see #setExistingApplication(IApplication)
	 */
	public void setExistingApplicationName(String applicationName) throws OpenShiftException {
		setExistingApplication(getApplicationByName(applicationName, getDomain()));
	}

	protected IApplication getApplicationByName(String applicationName, IDomain domain) {
		IApplication application = null;
		if (domain != null
				&& !StringUtils.isEmpty(applicationName)
				&& isExistingApplication(applicationName)) {
			application = domain.getApplicationByName(applicationName);
		}
		return application;
	}

	public void loadExistingApplications(IDomain domain) throws OpenShiftException {
		if (domain == null) {
			return;
		}
		setExistingApplications(domain.getApplications());
	}

	public void setResourcesLoaded(boolean loaded) {
		firePropertyChange(PROPERTY_RESOURCES_LOADED, this.resourcesLoaded, this.resourcesLoaded = loaded);
	}

	public boolean isResourcesLoaded() {
		return resourcesLoaded;
	}

	public boolean isExistingApplication(String applicationName) {
		return getExistingApplication(applicationName) != null;
	}

	public IApplication getExistingApplication(String applicationName) {
		IDomain domain = getDomain();
		if (domain == null
				|| applicationName == null) {
			return null;
		}
		
		return domain.getApplicationByName(applicationName);
	}
	
	public void setExistingApplications(List<IApplication> existingApplications) {
		firePropertyChange(PROPERTY_EXISTING_APPLICATIONS
				, this.existingApplications
				, this.existingApplications = existingApplications);
		
		if (getExistingApplicationName() != null
				&& !isExistingApplication(getExistingApplicationName())) {
			if (existingApplications != null
					&& !existingApplications.isEmpty()) {
				setExistingApplication(existingApplications.get(0));
			}
		}
	}

	public List<IApplication> getExistingApplications() {
		return existingApplications;
	}

	public void loadResources() throws OpenShiftException {
		setResourcesLoaded(false);
		
		loadDomains(getConnection());
		loadStandaloneCartridges(getConnection());
		loadEmbeddedCartridges(getConnection());
		loadExistingApplications(getDomain());
		loadGearProfiles(getDomain());

		setResourcesLoaded(true);
	}
	
	private void loadDomains(Connection connection) throws OpenShiftException {
		if (!isValid(connection)) {
			return;
		}
		List<IDomain> domains = connection.getDomains();
		setDomains(domains);
		setFirstDomainIfNoSet(getDomain(), connection);
	}

	private void loadStandaloneCartridges(Connection connection) throws OpenShiftException {
		if (!isValid(connection)) {
			return;
		}
		setCartridges(connection.getStandaloneCartridges());
	}

	public void setCartridges(List<IStandaloneCartridge> cartridges) {
		Collections.sort(cartridges, new CartridgeNameComparator());
		firePropertyChange(PROPERTY_CARTRIDGES, this.cartridges, this.cartridges = cartridges);
		
		IStandaloneCartridge cartridge = getSelectedCartridge();
		if (cartridge == null) {
			cartridge = getCartridgeByName(openShiftUserPreferencesProvider.getLastSelectedCartridgeName());
		}
		setSelectedCartridge(cartridge);
	}

	public List<IStandaloneCartridge> getCartridges() {
		return cartridges;
	}

	public IStandaloneCartridge getSelectedCartridge() {
		return wizardModel.getApplicationCartridge();
	}

	private void loadGearProfiles(IDomain domain) throws OpenShiftException {
		if (domain == null) {
			return;
		}
		setGearProfiles(domain.getAvailableGearProfiles());
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
	
	public void setSelectedCartridge(IStandaloneCartridge cartridge) {
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE
				, wizardModel.getApplicationCartridge()
				, wizardModel.setApplicationCartridge(cartridge));
		if(cartridge != null) {
			openShiftUserPreferencesProvider.setLastSelectedCartridgeName(cartridge.getName());
		}
	}

	private List<IEmbeddableCartridge> loadEmbeddedCartridges(Connection connection) throws OpenShiftException {
		if (!isValid(connection)) {
			return Collections.emptyList();
		}
		List<IEmbeddableCartridge> cartridges = connection.getEmbeddableCartridges();
		setEmbeddedCartridges(cartridges);
		return cartridges;
	}

	/**
	 * Sets the first application in the given domain as the existing
	 * application in this model. If there are no application in the given
	 * domain, then the existing application in this model is reseted.
	 * 
	 * @param domain of which the first application is used as existing application 
	 * @throws OpenShiftException
	 */
	public void setExistingApplication(IDomain domain) {
		IApplication existingApplication = null;
		List<IApplication> applications = domain.getApplications();
		if (applications != null
				&& !applications.isEmpty()) {
			existingApplication = applications.get(0);
		}
		setExistingApplication(existingApplication);
	}

	
	/**
	 * Sets the properties in this model that are related to an existing
	 * application. 
	 * 
	 * @param application
	 * @throws OpenShiftException
	 * 
	 * @see #setApplicationName(IApplication)
	 * @see #setSelectedEmbeddableCartridges(Set)
	 * @see #wizardModel#setApplication
	 */
	public void setExistingApplication(IApplication application) throws OpenShiftException {
		if (application != null) {
			doSetExistingApplication(
					application.getName(), application.getCartridge(), application.getEmbeddedCartridges(),
					application.getGearProfile(), application.getApplicationScale());
		} else {
			doSetExistingApplication(null, null, Collections.<IEmbeddedCartridge> emptyList(), null, null);
		}
		wizardModel.setApplication(application);
	}

	protected void doSetExistingApplication(String name, IStandaloneCartridge type,
			List<IEmbeddedCartridge> cartridges, IGearProfile gear, ApplicationScale scale)
			throws OpenShiftException {
		doSetExistingApplicationName(name);
		setApplicationName(name);
		setSelectedCartridge(type);
		setSelectedEmbeddableCartridges(cartridges);
		setSelectedGearProfile(gear);
		setScale(scale);
	}

	protected void doSetExistingApplicationName(String name) {
		this.existingApplicationName = name;
		firePropertyChange(PROPERTY_EXISTING_APPLICATION_NAME
				, this.existingApplicationName, this.existingApplicationName = name);
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

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	private void setFirstApplicationIfRequired(IApplication application, IDomain domain) {
		if (domain == null
				|| application == null) {
			return;
		}
		
		if (isExistingApplication(application.getName())) {
			return;
		}
		
		setExistingApplication(getFirstApplication(domain));
	}
	
	private IApplication getFirstApplication(IDomain domain) {
		if (domain == null) {
			return null;
		}
		
		List<IApplication> applications = domain.getApplications();
		if (applications == null
				|| applications.isEmpty()) {
			return null;
		}
		
		return applications.get(0);
	}
	
	public void setEmbeddedCartridges(List<IEmbeddableCartridge> cartridges) {
		firePropertyChange(PROPERTY_EMBEDDED_CARTRIDGES, null, this.embeddedCartridges = cartridges);
	}

	public List<IEmbeddableCartridge> getEmbeddedCartridges() {
		return embeddedCartridges;
	}

	@Override
	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException {
		getSelectedEmbeddableCartridges().add(cartridge);
		firePropertyChange(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, null, getSelectedEmbeddableCartridges());
	}

	@Override
	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) 
			throws OpenShiftException {
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

	public boolean isSelected(IEmbeddableCartridge cartridge) throws OpenShiftException {
		return getSelectedEmbeddableCartridges().contains(cartridge);
	}

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException {
		// no application yet, no cartridge embedded yet
		return false;
	}
	
	public boolean hasApplication(String applicationName) throws OpenShiftException {
		Connection connection = getConnection();
		if (connection == null
				|| !connection.isConnected()) {
			return false;
		}
		return connection.hasApplication(applicationName, getDomain());
	}

	public boolean isCurrentDomain(IDomain domain) {
		if (getDomain() == null) {
			return domain == null;
		} else {
			return getDomain().equals(domain);
		}
	}
	
	@Override
	public IDomain getDomain() throws OpenShiftException {
		return wizardModel.getDomain();
	}
	
	public void setDomain(IDomain domain) throws OpenShiftException {
		firePropertyChange(PROPERTY_DOMAIN, wizardModel.getDomain(), wizardModel.setDomain(domain));
		setFirstApplicationIfRequired(getApplication(), domain);	
		setEnvironmentVariablesSupported(isEnvironmentVariablesSupported());
	}

	public List<IDomain> getDomains() throws OpenShiftException {
		return wizardModel.getDomains();
	}
	
	private IDomain setFirstDomainIfNoSet(IDomain domain, Connection connection) {
		if (domain != null
				&& isExistingDomain(domain, connection)) {
			return domain;
		}
		
		domain = getFirstDomain(connection.getDomains());
		setDomain(domain);
		return domain;
	}

	private boolean isExistingDomain(IDomain domain, Connection connection) {
		for(IDomain availableDomain : connection.getDomains()) {
			if (availableDomain.equals(domain)) {
				return true;
			}
		}
		return false;
	}
	
	private IDomain getFirstDomain(List<IDomain> domains) {
		IDomain domain = null;
		if (domains != null
				&& !domains.isEmpty()) {
			domain = domains.get(0);
		}
		return domain;
	}
	
	public void setDomains(List<IDomain> domains) throws OpenShiftException {
		firePropertyChange(PROPERTY_DOMAINS, wizardModel.getDomains(), wizardModel.setDomains(domains));
	}
	
	public void refresh() throws OpenShiftException {
		IDomain domain = setFirstDomainIfNoSet(getDomain(), getConnection());
		if (isUseExistingApplication()
				&& !isExistingApplication(getExistingApplicationName())) {
			setExistingApplication(domain);
		}
	}
	
	/**
	 * Resets the settings for a new application (vs. existing application)
	 * 
	 * @throws OpenShiftException
	 */
	public void resetNewApplicationSettings() {
		setExistingApplication((IApplication) null); 
		setApplicationName("");
		setSelectedCartridge((IStandaloneCartridge) null);
		setSelectedEmbeddableCartridges(Collections.<IEmbeddableCartridge> emptyList());
		setSelectedGearProfile(null);
		resetInitialGitUrl();
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

	public Map<String, String> getEnvironmentVariables()	{
		return wizardModel.getEnvironmentVariables();
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES, 
				wizardModel.getEnvironmentVariables(),
				wizardModel.setEnvironmentVariables(environmentVariables));

	}
	
	public void setEnvironmentVariablesSupported(boolean supported) {
		firePropertyChange(PROPERTY_ENVIRONMENT_VARIABLES_SUPPORTED, null, isEnvironmentVariablesSupported());
	}

	public boolean isEnvironmentVariablesSupported() {
		return getDomain() != null
				&& getDomain().canCreateApplicationWithEnvironmentVariables();
	}

	public final OpenShiftApplicationWizardModel getWizardModel() {
		return wizardModel;
	}

}
