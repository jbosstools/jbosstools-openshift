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
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;

/**
 * @author Red Hat Developers
 *
 */
public class CreateServiceModel extends ProjectModel {
	public static final String PROPERTY_SERVICE_NAME = "serviceName";
	public static final String PROPERTY_SELECTED_SERVICE_TEMPLATE = "selectedServiceTemplate";
	public static final String PROPERTY_SELECTED_SERVICE_TEMPLATE_CRDS = "selectedServiceTemplateCRDs";
	public static final String PROPERTY_SELECTED_SERVICE_TEMPLATE_CRD = "selectedServiceTemplateCRD";
	

	private String serviceName = "";
	
	private final List<ServiceTemplate> serviceTemplates;
	
	private ServiceTemplate selectedServiceTemplate;
	
	private List<OperatorCRD> selectedServiceTemplateCRDs;
	
	private OperatorCRD selectedServiceTemplateCRD;
	
	/**
	 * @param odo
	 */
	public CreateServiceModel(Odo odo, List<ServiceTemplate> serviceTemplates, String projectName) {
		super(odo, projectName);
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
		setSelectedServiceTemplateCRDs(selectedServiceTemplate.getCRDs());
	}
	
	
	
	/**
	 * @return the selectedServiceTemplateCRS
	 */
	public List<OperatorCRD> getSelectedServiceTemplateCRDs() {
		return selectedServiceTemplateCRDs;
	}

	/**
	 * @param selectedServiceTemplateCRS the selectedServiceTemplateCRS to set
	 */
	public void setSelectedServiceTemplateCRDs(List<OperatorCRD> selectedServiceTemplateCRDs) {
		firePropertyChange(PROPERTY_SELECTED_SERVICE_TEMPLATE_CRDS, this.selectedServiceTemplateCRDs, this.selectedServiceTemplateCRDs = selectedServiceTemplateCRDs);
		if (!selectedServiceTemplateCRDs.isEmpty()) {
			setSelectedServiceTemplateCRD(selectedServiceTemplateCRDs.get(0));
		} else {
			setSelectedServiceTemplateCRD(null);
		}
	}

	/**
	 * @return the selectedServiceTemplateCRD
	 */
	public OperatorCRD getSelectedServiceTemplateCRD() {
		return selectedServiceTemplateCRD;
	}

	/**
	 * @param selectedServiceTemplateCRD the selectedServiceTemplateCRD to set
	 */
	public void setSelectedServiceTemplateCRD(OperatorCRD selectedServiceTemplateCRD) {
		firePropertyChange(PROPERTY_SELECTED_SERVICE_TEMPLATE_CRD, this.selectedServiceTemplateCRD, this.selectedServiceTemplateCRD = selectedServiceTemplateCRD);
	}

	/**
	 * @return the serviceTemplates
	 */
	public List<ServiceTemplate> getServiceTemplates() {
		return serviceTemplates;
	}
}
