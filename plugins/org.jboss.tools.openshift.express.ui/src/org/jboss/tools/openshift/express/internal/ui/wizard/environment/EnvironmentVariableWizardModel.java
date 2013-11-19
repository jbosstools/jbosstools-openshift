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
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public class EnvironmentVariableWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_VALUE = "value";

	private String name;
	private String value;

	private EnvironmentVariableItem variable;
	private AbstractEnvironmentVariablesWizardModel allVariablesModel;

	/**
	 * Creates a new wizard model for creating a new environment variable
	 * 
	 * @param application
	 *            the application that the environment variable shall get added
	 *            to
	 */
	public EnvironmentVariableWizardModel(AbstractEnvironmentVariablesWizardModel variablesModel) {
		this(new EnvironmentVariableItem(), variablesModel);
	}

	/**
	 * Creates a new wizard model for editing an exiting environment variable
	 * 
	 * @param variable
	 *            the environment variable that shall get edited
	 */
	public EnvironmentVariableWizardModel(EnvironmentVariableItem variable,
			AbstractEnvironmentVariablesWizardModel variablesModel) {
		this.variable = variable;
		this.name = variable.getName();
		this.value = variable.getValue();
		this.allVariablesModel = variablesModel;
	}

	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	public String getName() {
		return variable.getName();
	}

	public void setValue(String value) {
		firePropertyChange(PROPERTY_VALUE, this.value, this.value = value);
	}

	public String getValue() {
		return value;
	}

	public EnvironmentVariableItem getVariable() {
		return variable;
	}

	public boolean isExistingName(String name) {
		EnvironmentVariableItem variable = allVariablesModel.getVariable(name);
		return variable != null
				// ignore only if editing current one (not other one)
				&& !variable.equals(this.variable);
	}

	public boolean isEditing() {
		return allVariablesModel.contains(variable);
	}
	
	public void updateVariable() {
		variable.setName(name);
		variable.setValue(value);
		if (!isEditing()) {
			allVariablesModel.add(variable);
		}
		// TODO: find a way to fire a list member change so that we can get rid
		// of refreshing the viewer when we close the env var wizard
	}

}
