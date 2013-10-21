/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import com.openshift.client.IApplication;


/**
 * Used to persist information associated with the
 * ApplicationEnvironmentalVariableConfigurationWizardPage vew.
 * 
 * @author Martes G Wigglesworth
 */
public class ApplicationEnvironmentalVariableConfigurationWizardPageModel extends
		AbstractEnvironmentalVariablesWizardPageModel {

	/**
	 * Constructs a new instance of
	 * ApplicationEnvironmentalVariableConfigurationWizardPageModel
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel() {
		setPageTitle("Environmental Variable Configuration Wizard");
		setDescription("Used to configure application specific environmental variables for your OpenShift Gear");
	}

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPageModel
	 * @param iApplication
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel(IApplication iApplication) {
		super(iApplication);		
		setPageTitle("Environmental Variable Configuration Wizard");
		setDescription("Used to configure application specific environmental variables for:"+iApplication.getName());
	}

	
}
