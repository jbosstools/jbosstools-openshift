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

import java.util.List;

import org.jboss.tools.openshift.express.internal.core.connection.Connection;

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
	public static final String PAGE_DESCRIPTION = "Please provide name and value for your environment ";
	public static final String PAGE_NAME = "ApplicationEnvironmentalVariableEditWizardPage";
	
	// Properties for UI ease
	private final String PROPERTY_VARIABLE_NAME = "variableName";
	private final String PROPERTY_VARIABLE_VALUE = "variableValue";
	private final String PROPERTY_GRID_TITLE = "Edit Environment Variable";
	private final String PROPERTY_NAME_INPUT_TITLE = "Variable Name:";
	private final String PROPERTY_VALUE_INPUT_TITLE = "Variable Value:";
	
	private IEnvironmentVariable envVariable;
	
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel;
	
	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPageModel
	 */
	public ApplicationEnvironmentalVariableEditWizardPageModel() {
		// TODO Auto-generated constructor stub
	}
	
	public ApplicationEnvironmentalVariableEditWizardPageModel(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel) {
		super(PAGE_TITLE, null, confPageModel.getVariablesDB(), confPageModel.getSelectedVariable());
		this.envVariable = confPageModel.getSelectedVariable();
		this.confPageModel = confPageModel;
	}
	
	/**
	 * Update method to update the value in the EnvironmentalVariables HashMap
	 * using class variables
	 * @return
	 */
	public String update()
	{
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

}
