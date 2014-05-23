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

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftUserPreferencesProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.PojoEventBridge;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @author Martes G Wigglesworth
 */
public class ApplicationConfigurationWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_APPLICATION_SCALE = "scale";
	public static final String PROPERTY_CAN_ADDREMOVE_CARTRIDGES = "canAddRemoveCartridges";
	public static final String PROPERTY_DOMAIN = "domain";
	public static final String PROPERTY_DOMAINS = "domains";
	public static final String PROPERTY_EMBEDDED_CARTRIDGES = "embeddedCartridges";
	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";
	public static final String PROPERTY_ENVIRONMENT_VARIABLES_SUPPORTED = "environmentVariablesSupported";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_GEAR_PROFILES = "gearProfiles";
	public static final String PROPERTY_INITIAL_GITURL = "initialGitUrl";
	public static final String PROPERTY_RESOURCES_LOADED = "resourcesLoaded";
	public static final String PROPERTY_SCALABLE_APPLICATION = "scalableApplication";
	public static final String PROPERTY_SELECTED_APPLICATION_TEMPLATE = "selectedApplicationTemplate";
	public static final String PROPERTY_SELECTED_CARTRIDGE = "selectedCartridge";
	public static final String PROPERTY_SELECTED_GEAR_PROFILE = "selectedGearProfile";
	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String PROPERTY_USE_INITIAL_GITURL = "useInitialGitUrl";
	public static final String PROPERTY_IS_SOURCE_CODE_EDITABLE = "isSourceCodeEditable";

	private final OpenShiftApplicationWizardModel wizardModel;

	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private List<IGearProfile> gearProfiles = new ArrayList<IGearProfile>();
	private String existingApplicationName;
	private boolean resourcesLoaded = false;
	private ICartridge selectedCartridge;
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
				.listenTo(IOpenShiftApplicationWizardModel.PROP_EMBEDDED_CARTRIDGES, wizardModel)
				.forwardTo(PROPERTY_EMBEDDED_CARTRIDGES, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_USE_EXISTING_APPLICATION, wizardModel)
				.forwardTo(PROPERTY_USE_EXISTING_APPLICATION, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_EMBEDDED_CARTRIDGES, wizardModel)
				.forwardTo(PROPERTY_EMBEDDABLE_CARTRIDGES, this);
		new PojoEventBridge() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				firePropertyChange(PROPERTY_SELECTED_APPLICATION_TEMPLATE, event.getOldValue(), event.getNewValue());
				fireCanAddRemoveCartridges();
			}
		}
		.listenTo(IOpenShiftApplicationWizardModel.PROP_SELECTED_APPLICATION_TEMPLATE, wizardModel);
		
		new PojoEventBridge() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				firePropertyChange(PROPERTY_IS_SOURCE_CODE_EDITABLE, event.getOldValue(), event.getNewValue());
				fireIsSourceCodeEditable();
			}
		}
		.listenTo(IOpenShiftApplicationWizardModel.PROPERTY_IS_SOURCE_CODE_EDITABLE, wizardModel);
		
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_USE_INITIAL_GIT_URL, wizardModel)
				.forwardTo(PROPERTY_USE_INITIAL_GITURL, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_INITIAL_GIT_URL, wizardModel)
				.forwardTo(PROPERTY_INITIAL_GITURL, this);
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
		setEmbeddableCartridges(new ArrayList<ICartridge>(connection.getEmbeddableCartridges()));

		setResourcesLoaded(true);
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
		wizardModel.setAvailableEmbeddableCartridges(embeddableCartridges);
	}

	public List<ICartridge> getEmbeddableCartridges() {
		return wizardModel.getAvailableEmbeddableCartridges();
	}

	public void setEmbeddedCartridges(Set<ICartridge> selectedEmbeddableCartridges) {
		wizardModel.setEmbeddedCartridges(selectedEmbeddableCartridges);
	}

	public Set<ICartridge> getEmbeddedCartridges() throws OpenShiftException {
		return wizardModel.getEmbeddedCartridges();
	}

	public void addEmbeddedCartridges(ICartridge cartridge) throws OpenShiftException {
		wizardModel.addEmbeddedCartridges(Collections.<ICartridge> singletonList(cartridge));
	}

	public void removeEmbeddedCartridges(ICartridge cartridge) throws OpenShiftException {
		wizardModel.removeEmbeddedCartridges(Collections.<ICartridge> singletonList(cartridge));
	}

	public ICartridge getSelectedCartridge() throws OpenShiftException {
		return selectedCartridge;
	}

	public void setSelectedCartridge(ICartridge selectedEmbeddableCartridge) {
		firePropertyChange(PROPERTY_SELECTED_CARTRIDGE,
				this.selectedCartridge, this.selectedCartridge = selectedEmbeddableCartridge);
	}

	public void removeSelectedEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		wizardModel.removeEmbeddedCartridge(cartridge);
	}

	protected void fireCanAddRemoveCartridges() {
		firePropertyChange(PROPERTY_CAN_ADDREMOVE_CARTRIDGES, 
				!isCanAddRemoveCartridges(), isCanAddRemoveCartridges());
	}
	
	protected void fireIsSourceCodeEditable(){
		firePropertyChange(PROPERTY_IS_SOURCE_CODE_EDITABLE, 
				!isSourceCodeEditable(), isSourceCodeEditable());
		
	}

	/**
	 * Returns <code>true</code> if the user may modify the cartridges. This is
	 * not the case when we're using a quickstart. For quickstart the user may
	 * only choose among alternative, he may neither add/remove cartridges
	 * modify the downloadable cartridges.
	 * 
	 * @return
	 * 
	 * @see #getSelectedApplicationTemplate()
	 * @see IQuickstartApplicationTemplate
	 */
	public boolean isCanAddRemoveCartridges() {
		return getSelectedApplicationTemplate() != null
				&& getSelectedApplicationTemplate().canAddRemoveCartridges();
	}

	public boolean isSourceCodeEditable() {
		return getSelectedApplicationTemplate() != null
				&& getSelectedApplicationTemplate().isInitialGitUrlEditable();
	}
		
	public boolean hasInitialGitUrl() {
		return !StringUtils.isEmpty(getInitialGitUrl());
	}

	public boolean hasApplication(String applicationName) throws OpenShiftException {
		Connection connection = getConnection();
		if (wizardModel.isValid(connection)) {
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
	
	public boolean isUseInitialGitUrl() {
		return wizardModel.isUseInitialGitUrl();
	}
	
	public void setUseInitialGitUrl(boolean useInitialGitUrl) {
		setDefaultSourcecode(useInitialGitUrl);
	}

	public void setDefaultSourcecode(boolean useInitialGitUrl) {
		resetInitialGitUrl();
		wizardModel.setUseInitialGitUrl(useInitialGitUrl);
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
