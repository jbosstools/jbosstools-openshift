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

import java.util.HashMap;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

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
	public static final String PAGE_DESCRIPTION = "Please provide name and value for your environment ";
	public static final String PAGE_NAME = "ApplicationEnvironmentalVariableEditWizardPage";
	
	// Properties for UI ease
	private final String PROPERTY_VARIABLE_NAME = "variableName";
	private final String PROPERTY_VARIABLE_VALUE = "variableValue";
	private final String PROPERTY_GRID_TITLE = "Edit Environment Variable";
	private final String PROPERTY_NAME_INPUT_TITLE = "Variable Name:";
	private final String PROPERTY_VALUE_INPUT_TITLE = "Variable Value:";
	
	private String variableName;
	private String variableValue;
	
	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPageModel
	 * @param envVariables 
	 * @param variableName 
	 */
	public ApplicationEnvironmentalVariableEditWizardPageModel(String variableName, HashMap<String, String> envVariables) {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableEditWizardPageModel
	 * @param variableName
	 * @param variableValue
	 * @param wizardModel
	 */
	public ApplicationEnvironmentalVariableEditWizardPageModel(String variableName, String variableValue,ApplicationEnvironmentalVariableConfigurationWizardPageModel wizardModel) {
		//super.setVariablesDB(variablesDB);
	}
	
	
	/**
	 * Update method to update the value in the EnvironmentalVariables HashMap
	 * using class variables
	 * @return
	 */
	public String update()
	{
		return update(variableName, variableValue);
	}
	
	/**
	 * Update method to update the value in the EnvironmentalVariables HashMap
	 * @param target
	 * @param updateValue
	 * @return
	 */
	public String update(String target, String updateValue)
	{
		return super.updateKey(target, updateValue);
	}
	// #TODO - Create data bindings for all fields necessary for this view.
	
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
	
	/*
	 * Property values getters
	 */
	/**
	 * @return the pROPERTY_VARIABLE_NAME
	 */
	public String getPROPERTY_VARIABLE_NAME() {
		return PROPERTY_VARIABLE_NAME;
	}

	/**
	 * @return the pROPERTY_VARIABLE_VALUE
	 */
	public String getPROPERTY_VARIABLE_VALUE() {
		return PROPERTY_VARIABLE_VALUE;
	}
	
	/**
	 * @return the pROPERTY_GRID_TITLE
	 */
	public String getPROPERTY_GRID_TITLE() {
		return PROPERTY_GRID_TITLE;
	}

	/**
	 * @return the pROPERTY_NAME_INPUT_TITLE
	 */
	public String getPROPERTY_NAME_INPUT_TITLE() {
		return PROPERTY_NAME_INPUT_TITLE;
	}

	/**
	 * @return the pROPERTY_VALUE_INPUT_TITLE
	 */
	public String getPROPERTY_VALUE_INPUT_TITLE() {
		return PROPERTY_VALUE_INPUT_TITLE;
	}
	// #TODO - Create data bindings for all fields necessary for this view.

}
