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

import org.eclipse.jface.wizard.Wizard;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * @author Martin Rieman
 * @author Andre Dietisheim
 */
public class EnvironmentVariableWizard extends Wizard {

	private EnvironmentVariableWizardModel model;
	
	/**
	 * Used to create a new environment variable for the given application
	 * 
	 * @param application the application that we create an environment variable for
	 */
	public EnvironmentVariableWizard(EnvironmentVariablesWizardModel variablesModel) {
		this(new EnvironmentVariableItem(), variablesModel);
	}
	
	/**
	 * Used to edit an existing environment variable
	 * 
	 * @param variable the variable that shall get edited
	 * @param variablesModel 
	 */
	public EnvironmentVariableWizard(EnvironmentVariableItem variable, EnvironmentVariablesWizardModel variablesModel) {
		this.model = new EnvironmentVariableWizardModel(variable, variablesModel);
		setWindowTitle(variable);
		setNeedsProgressMonitor(true);
	}

	private void setWindowTitle(EnvironmentVariableItem variable) {
		if (variable == null) {
			setWindowTitle("Add Environment variable");
		} else {
			setWindowTitle("Edit Environment variable");
		}
	}

	@Override
	public boolean performFinish() {
		model.updateVariable();
		return true;
	}

	public void addPages() {
		addPage(new EnvironmentVariableWizardPage(model, this));
	}
	
	public EnvironmentVariableItem getVariable() {
		return model.getVariable();
	}
}
