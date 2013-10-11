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
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 *
 */
public class ApplicationEnvironmentalVariablesAddWizard extends Wizard {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariablesAddWizard
	 */
	public ApplicationEnvironmentalVariablesAddWizard() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		//The model should be updated in the body of this method.
		return true;
	}
	
	public void addPages()
	{
		addPage(new ApplicationEnvironmentalVariablesAddWizardPage("Variables Add Dialog", "Used to add a new variable to your application","New Variable Page", this));
	}

}
