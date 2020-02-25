/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;

/**
 * @author Red Hat Developers
 *
 */
public class CreateServiceModel extends ApplicationModel {
	public static final String PROPERTY_SERVICE_NAME = "serviceName";
	public static final String PROPERTY_SELECTED_SERVICE_TEMPLATE = "selectedServiceTemplate";
	

	private String serviceName = "";
	
	private final List<ServiceTemplate> serviceTemplates;
	
	private ServiceTemplate selectedServiceTemplate;
	
	/**
	 * @param odo
	 */
	public CreateServiceModel(Odo odo, List<ServiceTemplate> serviceTemplates, String projectName, String applicationName) {
		super(odo, projectName, applicationName);
		this.serviceTemplates = serviceTemplates;
		if (!serviceTemplates.isEmpty()) {
			setSelectedServiceTemplate(serviceTemplates.get(0));
		}
	}

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		firePropertyChange(PROPERTY_SERVICE_NAME, this.serviceName, this.serviceName = serviceName);
	}

	/**
	 * @return the selectedServiceTemplate
	 */
	public ServiceTemplate getSelectedServiceTemplate() {
		return selectedServiceTemplate;
	}

	/**
	 * @param selectedServiceTemplate the selectedServiceTemplate to set
	 */
	public void setSelectedServiceTemplate(ServiceTemplate selectedServiceTemplate) {
		firePropertyChange(PROPERTY_SELECTED_SERVICE_TEMPLATE, this.selectedServiceTemplate, this.selectedServiceTemplate = selectedServiceTemplate);
	}

	/**
	 * @return the serviceTemplates
	 */
	public List<ServiceTemplate> getServiceTemplates() {
		return serviceTemplates;
	}
}
