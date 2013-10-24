/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIException;

import com.openshift.client.IApplication;
import com.openshift.client.IEnvironmentVariable;


/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public class EnvironmentVariablesWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED = "selected";
	public static final String PROPERTY_VARIABLES = "variables";
	
	private List<EnvironmentVariableItem> variables = new ArrayList<EnvironmentVariableItem>();
	private EnvironmentVariableItem selected;
	private IApplication application;
	private Map<String, String> environmentVariables;
	
	/**
	 * Constructs a new model instance when no application but a map of variables is present
	 * 
	 * @param environmentVariables a map of key/values 
	 */
	public EnvironmentVariablesWizardModel(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	/**
	 * Constructs a new model instance when an application is present
	 * 
	 * @param environmentVariables a map of key/values 
	 */
	public EnvironmentVariablesWizardModel(IApplication application) {
		this.application = application;		
	}

	public void loadEnvironmentVariables() {
		initVariables(environmentVariables, application);
	}

	private void initVariables(Map<String, String> environmentVariables, IApplication application) {
		variables.clear();
		if (application != null) {
			initVariablesFor(application);
		} else {
			initVariablesFor(environmentVariables);
		}
	}

	private void initVariablesFor(Map<String, String> environmentVariables) {
		if (environmentVariables == null
				|| environmentVariables.isEmpty()) {
			return;
		}
		for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
			add(new EnvironmentVariableItem(entry.getKey(), entry.getValue()));
		}
	}

	private void initVariablesFor(IApplication application) {
		for (IEnvironmentVariable variable : application.getEnvironmentVariables().values()) {
			add(new EnvironmentVariableItem(variable.getName(), variable.getValue()));
		}
	}

	public List<EnvironmentVariableItem> getVariables() {
		return variables;
	}
	
	public EnvironmentVariableItem getSelected() {
		return selected;
	}

	public void setSelected(EnvironmentVariableItem variable) {
		firePropertyChange(PROPERTY_SELECTED, selected, this.selected = variable);;
	}

	public void add(EnvironmentVariableItem variable) {
		if (variables.contains(variable)) {
			throw new OpenShiftUIException("Cannot add a variable that already exists");
		}
		variables.add(variable);
		firePropertyChange(PROPERTY_VARIABLES, null, variables);
	}
	
	protected void remove(EnvironmentVariableItem variable) {
		variables.remove(variable);
		firePropertyChange(PROPERTY_VARIABLES, null, variables);
	}

	public IApplication getApplication() {
		return application;
	}

	public boolean contains(EnvironmentVariableItem variable) {
		return variables.contains(variable);
	}

	public EnvironmentVariableItem getVariable(String name) {
		for (EnvironmentVariableItem variable : variables) {
			if (variable.getName().equals(name)) {
				return variable;
			}
		}
		return null;
	}

	public boolean contains(String name) {
		return getVariable(name) != null;
	}
	
}
