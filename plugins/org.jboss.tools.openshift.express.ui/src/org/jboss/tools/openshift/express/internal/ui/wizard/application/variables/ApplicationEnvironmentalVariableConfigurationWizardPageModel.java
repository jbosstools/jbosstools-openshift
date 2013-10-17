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
		setPageTitle("Environmental Variables Configuration Wizard");
		setDescription("Used to configure application specific environmental variables for your OpenShift Gear");
	}

	public ApplicationEnvironmentalVariableConfigurationWizardPageModel(Connection newConnection) {
		setPageTitle("Environmental Variables Configuration Wizard");
		setUserConnection(newConnection);
	}

	public ApplicationEnvironmentalVariableConfigurationWizardPageModel(String pageTitle, Connection newConnection) {
		setPageTitle(pageTitle);
		setUserConnection(newConnection);
	}

	/**
	 * @return
	 */
	public String getSingleSelectedVariableName() {
		
		return selectionName;
	}

	private List<String> selectionNameList;
	
	public List<String> getSelectionList()
	{
		return selectionNameList;
	}

	private String selectionName;
	
	public void setSelection(String variableName)
	{
		 selectionName=variableName;
	}
	
	public void setSelection(List<String> variableNameList)
	{
		selectionNameList = variableNameList;
	}

	/**
	 * @return
	 */
	public String getSingleSelectedVariableValue() {
		return super.getVariablesDB().get(super.getVariablesDB().lastIndexOf(super.getSelectedVariable())).getValue();
	}
	
	public boolean isEmpty()
	{
		return !( super.getVariablesDB().isEmpty());
	}
}
