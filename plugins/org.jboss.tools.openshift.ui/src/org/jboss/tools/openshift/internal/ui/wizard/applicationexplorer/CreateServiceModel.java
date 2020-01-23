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
public class CreateServiceModel extends OdoModel {
	public static final String PROPERTY_SERVICE_NAME = "serviceName";
	public static final String PROPERTY_APPLICATION_NAME = "applicationName";
	public static final String PROPERTY_SELECTED_SERVICE_TEMPLATE = "selectedServiceTemplate";
	

	private String serviceName = "";
	
	private String applicationName = "";
	
	private String projectName;
	
	private final List<ServiceTemplate> serviceTemplates;
	
	private ServiceTemplate selectedServiceTemplate;
	
	/**
	 * @param odo
	 */
	public CreateServiceModel(Odo odo, List<ServiceTemplate> serviceTemplates, String project, String applicationName) {
		super(odo);
		this.serviceTemplates = serviceTemplates;
		this.projectName = project;
		this.applicationName = applicationName;
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
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return applicationName;
	}


	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		firePropertyChange(PROPERTY_APPLICATION_NAME, this.applicationName, this.applicationName = applicationName);
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


	/**
	 * @return the project name
	 */
	public String getProjectName() {
		return projectName;
	}

}
