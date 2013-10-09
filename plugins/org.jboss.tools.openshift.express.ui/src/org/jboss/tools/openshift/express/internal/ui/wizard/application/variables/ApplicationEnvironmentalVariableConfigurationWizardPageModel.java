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

import org.jboss.tools.openshift.express.internal.core.connection.Connection;


/**
 * 
 * @author Martes G Wigglesworth
 */
public class ApplicationEnvironmentalVariableConfigurationWizardPageModel extends AbstractEnvironmentalVariablesWizardPageModel {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPageModel
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel() {
		setPageTitle("Environmental Variables Configuration Wizard");
	}
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel(Connection newConnection) {
		setPageTitle("Environmental Variables Configuration Wizard");setUserConnection(newConnection);
	}
	
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel(String pageTitle,Connection newConnection) {
		setPageTitle(pageTitle);setUserConnection(newConnection);
	}
	// #TODO - Create data bindings for all fields necessary for this view.
	
}
