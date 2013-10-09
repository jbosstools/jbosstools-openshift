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

import org.eclipse.jface.wizard.Wizard;

/**
 * @author Martes G Wigglesworth
 * 
 */
public class ApplicationEnvironmentalVariablesWizard extends Wizard {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariablesWizard
	 */
	public ApplicationEnvironmentalVariablesWizard() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addPages() {
		addPage(new ApplicationEnvironmentalVariableConfigurationWizardPage("Variables View Dialog","Used to create and edit variables for OpenShift Applications", "Variables Table View", this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		return true;
	}
}
