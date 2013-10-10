/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import java.util.HashMap;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author Martin Rieman
 */
public class ApplicationEnvironmentalVariableEditWizard extends Wizard {

	//private ApplicationEnvironmentalVariableEditWizardPage applicationEnvironmentalVariableEditWizardPage;
	private Connection user;
	private String variableName;
	private HashMap<String, String> envVariables;
	private ApplicationEnvironmentalVariableEditWizardPage applicationEnvironmentalVariableEditWizardPage;
	
	public ApplicationEnvironmentalVariableEditWizard(Connection user, String variableName, HashMap<String, String> envVariables) {
		this.user = user;
		this.variableName = variableName;
		this.envVariables = envVariables;
		setWindowTitle(ApplicationEnvironmentalVariableEditWizardPageModel.PAGE_TITLE);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		applicationEnvironmentalVariableEditWizardPage.getPageModel().update();
		return true;
	}

	@Override
	public void addPages() {
		applicationEnvironmentalVariableEditWizardPage = new ApplicationEnvironmentalVariableEditWizardPage(user, variableName, envVariables, this);
		addPage(applicationEnvironmentalVariableEditWizardPage);
	}
}
