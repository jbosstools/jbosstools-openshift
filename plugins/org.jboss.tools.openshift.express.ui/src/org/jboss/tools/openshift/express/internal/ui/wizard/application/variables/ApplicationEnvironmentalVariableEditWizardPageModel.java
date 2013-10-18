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
 * @author Martes G Wigglesworth
 * @author Martin Rieman
 * 
 * Page Model for editing a variable in the Application Environment Variables
 *
 */
public class ApplicationEnvironmentalVariableEditWizardPageModel extends AbstractEnvironmentalVariablesWizardPageModel {

	// Page Properties
	public static final String PAGE_TITLE = "Edit Environment Variable";
	public static final String PAGE_DESCRIPTION = "Used to edit current Environment Variable associated with: ";
	public static final String PAGE_NAME = "ApplicationEnvironmentalVariableEditWizardPage";
	
	// Properties for UI ease
	public static final String PROPERTY_VARIABLE_NAME = "variableName";
	public static final String PROPERTY_VARIABLE_VALUE = "variableValue";
	public static final String PROPERTY_GRID_TITLE = "Edit Environment Variable";
	public static final String PROPERTY_NAME_INPUT_TITLE = "Variable Name:";
	public static final String PROPERTY_VALUE_INPUT_TITLE = "Variable Value:";
	
	private IEnvironmentVariable envVariable;
	private String variableName;
	private String variableValue;
	
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel;
	
	/**
	 * 
	 * Constructs a new instance of ApplicationEnvironmentalVariableEditWizardPageModel
	 * @param confPageModel
	 */
	public ApplicationEnvironmentalVariableEditWizardPageModel(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel) {
		super(PAGE_TITLE, confPageModel.getVariablesDB(), confPageModel.getSelectedVariable());
		this.envVariable = confPageModel.getSelectedVariable();
		this.confPageModel = confPageModel;
		setDescription(PAGE_DESCRIPTION+confPageModel.getIApplication().getName());
		this.variableName = this.envVariable.getName();
		this.variableValue = this.envVariable.getValue();
	}
	
	/**
	 * Update method to update the value in the EnvironmentalVariables HashMap
	 * using class variables
	 * @return
	 */
	public String update()
	{
		envVariable.update(variableValue);
		confPageModel.getVariablesDB().get(confPageModel.getVariablesDB().lastIndexOf(confPageModel.getSelectedVariable())).update(envVariable.getValue());
		confPageModel.setSelectedVariable(envVariable);
		return super.update(envVariable);
	}
	
	/**
	 * @return the envVariable
	 */
	public IEnvironmentVariable getEnvVariable() {
		return envVariable;
	}

	/**
	 * @param envVariable the envVariable to set
	 */
	public void setEnvVariable(IEnvironmentVariable envVariable) {
		this.envVariable = envVariable;
	}

	/**
	 * @return the variableName
	 */
	public String getVariableName() {
		return variableName;
	}

	/**
	 * @param variableName the variableName to set
	 */
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	/**
	 * @return the variableValue
	 */
	public String getVariableValue() {
		return variableValue;
	}

	/**
	 * @param variableValue the variableValue to set
	 */
	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}

}
