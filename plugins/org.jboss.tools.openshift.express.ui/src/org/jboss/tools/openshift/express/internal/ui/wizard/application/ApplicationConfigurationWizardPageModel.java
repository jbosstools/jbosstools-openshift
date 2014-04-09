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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftUserPreferencesProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.PojoEventBridge;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @author Martes G Wigglesworth
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_APPLICATION_SCALE = "scale";
	public static final String PROPERTY_DEFAULT_SOURCECODE = "defaultSourcecode";
	public static final String PROPERTY_DOMAIN = "domain";
	public static final String PROPERTY_DOMAINS = "domains";
	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES_SUPPORTED = "environmentVariablesSupported";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_GEAR_PROFILES = "gearProfiles";
	public static final String PROPERTY_INITIAL_GITURL = "initialGitUrl";
	public static final String PROPERTY_RESOURCES_LOADED = "resourcesLoaded";
	public static final String PROPERTY_SCALABLE_APPLICATION = "scalableApplication";
	public static final String PROPERTY_SELECTED_STANDALONE_CARTRIDGE = "selectedStandaloneCartridge";
	public static final String PROPERTY_SELECTED_APPLICATION_TEMPLATE = "selectedApplicationTemplate";
	public static final String PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";
	public static final String PROPERTY_SELECTED_GEAR_PROFILE = "selectedGearProfile";
	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";

	private final OpenShiftApplicationWizardModel wizardModel;

	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<IGearProfile> gearProfiles = new ArrayList<IGearProfile>();
	private String existingApplicationName;
	private boolean resourcesLoaded = false;
	private boolean defaultSourcecode = true;
	private OpenShiftUserPreferencesProvider openShiftUserPreferencesProvider = new OpenShiftUserPreferencesProvider();

	protected ApplicationConfigurationWizardPageModel(OpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setupWizardModelListeners(wizardModel);
	}

	protected void setupWizardModelListeners(OpenShiftApplicationWizardModel wizardModel) {
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_DOMAIN, wizardModel)
				.forwardTo(PROPERTY_DOMAIN, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_DOMAINS, wizardModel)
				.forwardTo(PROPERTY_DOMAINS, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_APPLICATION_NAME, wizardModel)
				.forwardTo(PROPERTY_APPLICATION_NAME, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_SELECTED_APPLICATION_TEMPLATE, wizardModel)
				.forwardTo(PROPERTY_SELECTED_APPLICATION_TEMPLATE, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_USE_EXISTING_APPLICATION, wizardModel)
				.forwardTo(PROPERTY_USE_EXISTING_APPLICATION, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_SELECTED_EMBEDDABLE_CARTRIDGES, wizardModel)
				.forwardTo(PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES, this);
	}

	public Connection getConnection() {
		return wizardModel.getConnection();
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

	protected IApplication getApplicationByName(String applicationName, IDomain domain) {
		IApplication application = null;
		if (domain != null
				&& !StringUtils.isEmpty(applicationName)
				&& isExistingApplication(applicationName)) {
			application = domain.getApplicationByName(applicationName);
		}
		return application;
	}

	public void loadExistingApplications() throws OpenShiftException {
		IDomain domain = getDomain();
		if (domain == null) {
			return;
		}

		setResourcesLoaded(false);
		
		setExistingApplications(domain.getApplications());
		
		setResourcesLoaded(true);
	}

	public void setResourcesLoaded(boolean loaded) {
		firePropertyChange(PROPERTY_RESOURCES_LOADED, this.resourcesLoaded, this.resourcesLoaded = loaded);
	}

	public boolean isResourcesLoaded() {
		return resourcesLoaded;
	}

	public boolean isExistingApplication(String applicationName) {
		return getExistingApplication(applicationName, getDomain()) != null;
	}

	private IApplication getExistingApplication(String applicationName, IDomain domain) {
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
	}

	public List<IApplication> getExistingApplications() {
		return existingApplications;
	}

	public void loadResources() throws OpenShiftException {
		Connection connection = getConnection();
		if (!wizardModel.isValid(connection)) {
			return;
		}

		setResourcesLoaded(false);

		loadGearProfiles(getDomain());
		setEmbeddableCartridges(new ArrayList<ICartridge>(getConnection().getEmbeddableCartridges()));

		setResourcesLoaded(true);
	}

	public IStandaloneCartridge getSelectedStandaloneCartridge() {
		return wizardModel.getStandaloneCartridge();
	}

	public IApplicationTemplate getSelectedApplicationTemplate() {
		return wizardModel.getSelectedApplicationTemplate();
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

	public void setApplicationName(String applicationName) {
		wizardModel.setApplicationName(applicationName);
	}

	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}

	public void setEmbeddableCartridges(List<ICartridge> embeddableCartridges) {
		wizardModel.setEmbeddableCartridges(embeddableCartridges);
	}

	public List<ICartridge> getEmbeddableCartridges() {
		return wizardModel.getEmbeddableCartridges();
	}

	public Set<ICartridge> getSelectedEmbeddableCartridges() throws OpenShiftException {
		return wizardModel.getSelectedEmbeddableCartridges();
	}

	public void setSelectedEmbeddableCartridges(Set<ICartridge> selectedEmbeddableCartridges) {
		wizardModel.setSelectedEmbeddableCartridges(selectedEmbeddableCartridges);
	}

	public void removeSelectedEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		wizardModel.removeSelectedEmbeddableCartridge(cartridge);
	}

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException {
		return getSelectedEmbeddableCartridges().contains(cartridge);
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

	public IDomain getDomain() throws OpenShiftException {
		return wizardModel.getDomain();
	}

	public void setDomain(IDomain domain) throws OpenShiftException {
		wizardModel.setDomain(domain);
		setEnvironmentVariablesSupported(isEnvironmentVariablesSupported());
	}

	public List<IDomain> getDomains() throws OpenShiftException {
		return wizardModel.getDomains();
	}

	public void setDomains(List<IDomain> domains) throws OpenShiftException {
		wizardModel.setDomains(domains);
	}
	
	public boolean isDefaultSourcecode() {
		return defaultSourcecode;
	}

	public void setDefaultSourcecode(boolean defaultSourcecode) {
		resetInitialGitUrl();
		firePropertyChange(PROPERTY_DEFAULT_SOURCECODE, this.defaultSourcecode,
				this.defaultSourcecode = defaultSourcecode);
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

	public Map<String, String> getEnvironmentVariables() {
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
	
	public boolean isUseExistingApplication() {
		return wizardModel.isUseExistingApplication();
	}
}
