/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import org.eclipse.jface.wizard.Wizard;

/**
 * @author Martin Rieman <mrieman@redhat.com>
 */
public class ApplicationEnvironmentalVariableEditWizard extends Wizard {

	private ApplicationEnvironmentalVariableEditWizardPage applicationEnvironmentalVariableEditWizardPage;
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel;
	
	public ApplicationEnvironmentalVariableEditWizard(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel) {
		this.confPageModel = confPageModel;
		setWindowTitle(ApplicationEnvironmentalVariableEditWizardPageModel.PAGE_TITLE);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		applicationEnvironmentalVariableEditWizardPage.getPageModel().update();
		confPageModel.setVariablesDB(applicationEnvironmentalVariableEditWizardPage.getPageModel().getVariablesDB());
		confPageModel.setSelectedVariable(applicationEnvironmentalVariableEditWizardPage.getPageModel().getSelectedVariable());
		
		return true;
	}

	@Override
	public void addPages() {
		applicationEnvironmentalVariableEditWizardPage = new ApplicationEnvironmentalVariableEditWizardPage(confPageModel, this);
		addPage(applicationEnvironmentalVariableEditWizardPage);
	}
	
	/**
	 * @return the confPageModel
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPageModel getConfPageModel() {
		return confPageModel;
	}
}
