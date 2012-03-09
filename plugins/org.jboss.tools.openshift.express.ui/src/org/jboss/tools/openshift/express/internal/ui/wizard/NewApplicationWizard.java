/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.express.client.IApplication;

/**
 * @author André Dietisheim
 */
public class NewApplicationWizard extends Wizard {

	private ApplicationWizardModel wizardModel;
	private NewApplicationWizardPage applicationPage;
	private EmbedCartridgeWizardPage embedCartridgePage;

	public NewApplicationWizard(UserDelegate user) {
		this.wizardModel = new ApplicationWizardModel(user);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		boolean successfull = true;
		if (wizardModel.getApplication() == null) {
			successfull = applicationPage.createApplication();
		}
		if (successfull) {
			successfull = embedCartridgePage.processCartridges();
		}
		return successfull;
	}

	@Override
	public void addPages() {
		addPage(this.applicationPage = new NewApplicationWizardPage(wizardModel, this));
		addPage(this.embedCartridgePage = new EmbedCartridgeWizardPage(wizardModel, this));
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}
}
