/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
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
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.CartridgeNameComparator;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftUserPreferencesProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.PojoEventBridge;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.CartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.DownloadableCartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateWizardPageModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION_TEMPLATES = "applicationTemplates";
	public static final String PROPERTY_EXISTING_APPLICATIONS = "existingApplications";
	public static final String PROPERTY_DOMAINS = "domains";
	public static final String PROPERTY_EXISTING_APPLICATION_NAME = "existingApplicationName";
	public static final String PROPERTY_EXISTING_APPLICATION = "existingApplication";
	public static final String PROPERTY_RESOURCES_LOADED = "resourcesLoaded";
	public static final String PROPERTY_SELECTED_APPLICATION_TEMPLATE = "selectedApplicationTemplate";
	public static final String PROPERTY_USE_EXISTING_APPLICATION = "useExistingApplication";

	private final OpenShiftApplicationWizardModel wizardModel;

	// start with a null value as a marker of non-initialized state (used during
	// first pass validation)
	private String existingApplicationName;
	private List<IApplication> existingApplications = new ArrayList<IApplication>();
	private boolean resourcesLoaded = false;
	private List<IApplicationTemplate> applicationTemplates = new ArrayList<IApplicationTemplate>();
	private OpenShiftUserPreferencesProvider openShiftUserPreferencesProvider = new OpenShiftUserPreferencesProvider();

	protected ApplicationTemplateWizardPageModel(OpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
		setExistingApplication(wizardModel.getApplication());
		setUseExistingApplication(wizardModel.isUseExistingApplication());
		setupWizardModelListeners(wizardModel);
	}

	private void setupWizardModelListeners(OpenShiftApplicationWizardModel wizardModel) {
		wizardModel.addPropertyChangeListener(IOpenShiftApplicationWizardModel.PROP_DOMAIN,
				new PropertyChangeListener() {

					@SuppressWarnings("unchecked")
					public void propertyChange(PropertyChangeEvent event) {
						Object newValue = event.getNewValue();
						if (!(newValue instanceof List)) {
							return;
						}

						setExistingApplicationsFor((List<IDomain>) newValue);
					}
				});
		new PojoEventBridge()
			.listenTo(IOpenShiftApplicationWizardModel.PROP_USE_EXISTING_APPLICATION, wizardModel)
			.forwardTo(PROPERTY_USE_EXISTING_APPLICATION, this);
		new PojoEventBridge()
				.listenTo(IOpenShiftApplicationWizardModel.PROP_APPLICATION, wizardModel)
				.forwardTo(PROPERTY_EXISTING_APPLICATION, this);
	}

	public Connection getConnection() {
		return wizardModel.getConnection();
	}

	public String[] getExistingApplicationNames() {
		try {
			List<IApplication> applications = getExistingApplications();
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

	public List<IApplicationTemplate> getApplicationTemplates() {
		if (applicationTemplates == null) {
			this.applicationTemplates = createApplicationTemplates(createCartridgeApplicationTemplates(getStandaloneCartridges()));
		}
		return applicationTemplates;
	}

	protected List<IApplicationTemplate> createApplicationTemplates(
			List<CartridgeApplicationTemplate> standaloneCartridges) {
		List<IApplicationTemplate> applicationTemplates = new ArrayList<IApplicationTemplate>();
		applicationTemplates.addAll(standaloneCartridges);
		applicationTemplates
				.add(new DownloadableCartridgeApplicationTemplate());

		return applicationTemplates;
	}

	private List<CartridgeApplicationTemplate> createCartridgeApplicationTemplates(List<IStandaloneCartridge> cartridges) {
		List<CartridgeApplicationTemplate> cartridgeApplicationTemplates = new ArrayList<CartridgeApplicationTemplate>();
		for (IStandaloneCartridge cartridge : cartridges) {
			cartridgeApplicationTemplates.add(new CartridgeApplicationTemplate(cartridge));
		}
		return cartridgeApplicationTemplates;

	}

	private List<IApplicationTemplate> setApplicationTemplates(List<IApplicationTemplate> applicationTemplates) {
		firePropertyChange(PROPERTY_APPLICATION_TEMPLATES, null, this.applicationTemplates = applicationTemplates);
		return applicationTemplates;
	}

	public boolean isUseExistingApplication() {
		return wizardModel.isUseExistingApplication();
	}

	public void setUseExistingApplication(boolean useExistingApplication) {
		firePropertyChange(PROPERTY_USE_EXISTING_APPLICATION
				, wizardModel.isUseExistingApplication()
				, wizardModel.setUseExistingApplication(useExistingApplication));
	}

	public String getExistingApplicationName() {
		return existingApplicationName;
	}

	/**
	 * Sets the existing application in this model for the given application
	 * name. If there's an existing application with the given name, all
	 * properties related to an existing application are also set.
	 * 
	 * @param applicationName
	 * @throws OpenShiftException
	 * 
	 * @see #setExistingApplication(IApplication)
	 */
	public void setExistingApplicationName(String applicationName) throws OpenShiftException {
		setExistingApplication(getApplicationByName(applicationName, getDomains()));
	}

	protected IApplication getApplicationByName(String applicationName, List<IDomain> domains) {
		IApplication matchingApplication = null;
		if (domains != null
				&& !StringUtils.isEmpty(applicationName)) {
			for (IDomain domain : domains) {
				IApplication application = domain.getApplicationByName(applicationName);
				if (application != null) {
					matchingApplication = application;
					break;
				}
			}
		}
		return matchingApplication;
	}

	public void setExistingApplication(IApplication application) throws OpenShiftException {
		wizardModel.setApplication(application);
		if (application != null) {
			doSetExistingApplicationName(application.getName());
			setUseExistingApplication(true);
		} else {
			doSetExistingApplicationName(null);
		}
	}

	protected void doSetExistingApplicationName(String name) {
		firePropertyChange(PROPERTY_EXISTING_APPLICATION_NAME,
				this.existingApplicationName, this.existingApplicationName = name);
	}

	public IApplication getExistingApplication() {
		return wizardModel.getApplication();
	}

	public void setResourcesLoaded(boolean loaded) {
		firePropertyChange(PROPERTY_RESOURCES_LOADED, this.resourcesLoaded, this.resourcesLoaded = loaded);
	}

	public boolean isResourcesLoaded() {
		return resourcesLoaded;
	}

	public void loadResources() throws OpenShiftException {
		Connection connection = getConnection();
		if (!wizardModel.isValid(connection)) {
			return;
		}

		setResourcesLoaded(false);

		ensureHasDomain();
		setDomains(connection.getDomains());
		setStandaloneCartridges(connection.getStandaloneCartridges());

		setResourcesLoaded(true);
	}

	protected void setStandaloneCartridges(List<IStandaloneCartridge> cartridges) {
		cartridges = new ArrayList<IStandaloneCartridge>(cartridges);
		Collections.sort(cartridges, new CartridgeNameComparator());
		wizardModel.setStandaloneCartridges(cartridges);
		setApplicationTemplates(createApplicationTemplates(createCartridgeApplicationTemplates(cartridges)));
	}

	protected List<IStandaloneCartridge> getStandaloneCartridges() {
		return wizardModel.getStandaloneCartridges();
	}

	protected void setDomains(List<IDomain> domains) {
		firePropertyChange(PROPERTY_DOMAINS, wizardModel.getDomains(), wizardModel.setDomains(domains));
		setExistingApplicationsFor(domains);
	}

	protected void ensureHasDomain() {
		wizardModel.ensureHasDomain();
	}

	public IApplicationTemplate getSelectedApplicationTemplate() {
		return wizardModel.getSelectedApplicationTemplate();
	}

	public void setSelectedApplicationTemplate(IApplicationTemplate template) {
		firePropertyChange(PROPERTY_SELECTED_APPLICATION_TEMPLATE
				, wizardModel.getSelectedApplicationTemplate()
				, wizardModel.setSelectedApplicationTemplate(template));
		setExistingApplicationName(null);
	}

	public List<IDomain> getDomains() throws OpenShiftException {
		return wizardModel.getDomains();
	}

	protected void setExistingApplicationsFor(List<IDomain> domains) throws OpenShiftException {
		List<IApplication> existingApplications = new ArrayList<IApplication>();
		if (domains != null) {
			for (IDomain domain : domains) {
				// long-running
				existingApplications.addAll(domain.getApplications());
			}
		}
		setExistingApplications(existingApplications);
	}

	public void setExistingApplications(List<IApplication> applications) throws OpenShiftException {
		firePropertyChange(PROPERTY_EXISTING_APPLICATIONS,
				this.existingApplications, this.existingApplications = applications);
	}

	public List<IApplication> getExistingApplications() throws OpenShiftException {
		return existingApplications;
	}

	public final OpenShiftApplicationWizardModel getWizardModel() {
		return wizardModel;
	}
}
