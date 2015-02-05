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

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.ui.ExpressException;


/**
 * Wizard that lists the environment variables and edit, add, remove them.
 * 
 * @author Martes G Wigglesworth
 * @author Andre Dietisheim
 */
public abstract class AbstractEnvironmentVariablesWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_SELECTED = "selected";
	public static final String PROPERTY_VARIABLES = "variables";
	public static final String PROPERTY_SUPPORTED = "supported";
	
	private List<EnvironmentVariableItem> variables = new ArrayList<EnvironmentVariableItem>();
	private EnvironmentVariableItem selected;
	
	public abstract void refreshEnvironmentVariables();

	public abstract void loadEnvironmentVariables();

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
			throw new ExpressException("Cannot add a variable that already exists");
		}
		variables.add(variable);
		firePropertyChange(PROPERTY_VARIABLES, null, variables);
	}
	
	protected void remove(EnvironmentVariableItem variable) {
		variables.remove(variable);
		firePropertyChange(PROPERTY_VARIABLES, null, variables);
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
	
	public abstract boolean isSupported();
	
	public abstract String getHost();
	
	protected void clear() {
		variables.clear();
		firePropertyChange(PROPERTY_VARIABLES, null, variables);
	}
}
