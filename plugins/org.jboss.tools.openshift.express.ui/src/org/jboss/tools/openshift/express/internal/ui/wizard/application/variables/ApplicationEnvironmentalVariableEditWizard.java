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

import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IEnvironmentVariable;

/**
 * @author Martin Rieman
 */
public class ApplicationEnvironmentalVariableEditWizard extends Wizard {

	//private ApplicationEnvironmentalVariableEditWizardPage applicationEnvironmentalVariableEditWizardPage;
	private Connection user;
	private IEnvironmentVariable envVariable;
	private List<IEnvironmentVariable> envVariables;
	private ApplicationEnvironmentalVariableEditWizardPage applicationEnvironmentalVariableEditWizardPage;
	
	public ApplicationEnvironmentalVariableEditWizard(Connection user, IEnvironmentVariable envVariable, List<IEnvironmentVariable> envVariables) {
		this.user = user;
		this.envVariable = envVariable;
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
		applicationEnvironmentalVariableEditWizardPage = new ApplicationEnvironmentalVariableEditWizardPage(user, envVariable, envVariables, this);
		addPage(applicationEnvironmentalVariableEditWizardPage);
	}
}
