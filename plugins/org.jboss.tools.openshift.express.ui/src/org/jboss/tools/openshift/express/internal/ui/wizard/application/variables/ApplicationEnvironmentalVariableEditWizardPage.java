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
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Martes G Wigglesworth
 *
 */
public class ApplicationEnvironmentalVariableEditWizardPage extends AbstractOpenShiftWizardPage {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPage
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariableEditWizardPage(String title, String description, String pageName,
			IWizard wizard) {
		super(title, description, pageName, wizard);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage#doCreateControls(org.eclipse.swt.widgets.Composite, org.eclipse.core.databinding.DataBindingContext)
	 */
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		// TODO Auto-generated method stub

	}

}
