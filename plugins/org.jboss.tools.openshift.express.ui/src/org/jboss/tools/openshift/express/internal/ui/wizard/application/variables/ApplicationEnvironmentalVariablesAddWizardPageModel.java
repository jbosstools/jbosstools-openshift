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
	public static final String PAGE_DESCRIPTION = "Use this view to add an Environment Variable to: ";
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
		/*
		 * A messy attempt to quickly insert the current IApplication's name into the page 
		 * description for this view. (Does not currently function since this constructor is not called.)
		 */
		this.setDescription(PAGE_DESCRIPTION+ confPageModel.getIApplication().getName());
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
		return returnString;
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
