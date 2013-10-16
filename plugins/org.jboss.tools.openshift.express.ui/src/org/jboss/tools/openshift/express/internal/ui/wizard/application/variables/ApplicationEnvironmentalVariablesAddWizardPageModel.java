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

import com.openshift.client.IEnvironmentVariable;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * @author Martin Rieman
 *
 */
public class ApplicationEnvironmentalVariablesAddWizardPageModel extends AbstractEnvironmentalVariablesWizardPageModel {

	// Page Properties
	public static final String PAGE_TITLE = "Variables Add Dialog";
	public static final String PAGE_DESCRIPTION = "Used to add a new variable to your application";
	public static final String PAGE_NAME = "New Variable Page";
	
	// Properties for UI ease
	public static final String PROPERTY_VARIABLE_NAME = "variableName";
	public static final String PROPERTY_VARIABLE_VALUE = "variableValue";
	
	private IEnvironmentVariable envVariable;
	private String variableName;
	private String variableValue;
	
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel;
	
	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariablesAddWizardPageModel
	 * @param confPageModel2
	 */
	public ApplicationEnvironmentalVariablesAddWizardPageModel(
			ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel) {
		super(PAGE_TITLE, confPageModel.getVariablesDB(), confPageModel.getSelectedVariable());
		this.envVariable = confPageModel.getSelectedVariable();
		this.confPageModel = confPageModel;
	}

	/**
	 * Update method to add the value in the EnvironmentalVariables list
	 * using class variables
	 * @return
	 */
	public String add()	{
		envVariable = initializeIEnvironmentVariable(variableName, variableValue);
		String returnString = super.add(envVariable);
		confPageModel.setVariablesDB(getVariablesDB());
		confPageModel.setSelectedVariable(getSelectedVariable());
		return returnString;
	}

}
