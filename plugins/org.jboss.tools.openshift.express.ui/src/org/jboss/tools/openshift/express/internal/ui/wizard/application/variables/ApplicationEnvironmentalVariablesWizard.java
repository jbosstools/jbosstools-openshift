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


import org.eclipse.core.databinding.DataBindingContext;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IApplication;

/**
 * @author Martes G Wigglesworth
 * 
 */
public class ApplicationEnvironmentalVariablesWizard extends AbstractOpenShiftWizard<IApplication> {	

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariablesWizard
	 * @param title
	 * @param model
	 */
	public ApplicationEnvironmentalVariablesWizard(String wizardTitle, IApplication model) {
		super("Manage Application Environment Variable(s)", model);
		
		
	}
	
	

	@Override
	public void addPages() {
		
		addPage(new ApplicationEnvironmentalVariableConfigurationWizardPage("Variables View Dialog",
				"Used to create and edit variables for: "+ getModel().getName(), "Variables Table View", this,getModel()));
	}

	/**
	 * 
	 * @return
	 */
	public DataBindingContext getDataBindingContext()
	{
		return dbc;
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


	private DataBindingContext dbc;


}
