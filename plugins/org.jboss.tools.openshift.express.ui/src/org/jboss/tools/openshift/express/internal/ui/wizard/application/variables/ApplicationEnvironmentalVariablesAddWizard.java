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
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * @author Martin Rieman
 * 
 */
public class ApplicationEnvironmentalVariablesAddWizard extends Wizard {

	private ApplicationEnvironmentalVariablesAddWizardPage applicationEnvironmentalVariablesAddWizardPage;
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel;

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariablesAddWizard
	 */
	public ApplicationEnvironmentalVariablesAddWizard(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel) {
		this.confPageModel = confPageModel;
		setWindowTitle(ApplicationEnvironmentalVariablesAddWizardPageModel.PAGE_TITLE);
		setNeedsProgressMonitor(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		applicationEnvironmentalVariablesAddWizardPage.getPageModel().add();
		//TODO put refresh here!!!
		return true;
	}

	public void addPages()
	{
		applicationEnvironmentalVariablesAddWizardPage = new ApplicationEnvironmentalVariablesAddWizardPage(confPageModel, this);
		addPage(applicationEnvironmentalVariablesAddWizardPage);
	}

}
