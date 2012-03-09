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
public class EmbedCartridgeWizard extends Wizard {

	private ApplicationWizardModel wizardModel;
	private EmbedCartridgeWizardPage embedCartridgeWizardPage;

	public EmbedCartridgeWizard(IApplication application, UserDelegate user) {
		this.wizardModel = new ApplicationWizardModel(application, user);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return embedCartridgeWizardPage.processCartridges();
	}

	@Override
	public void addPages() {
		addPage(this.embedCartridgeWizardPage = new EmbedCartridgeWizardPage(wizardModel, this));
	}

	public IApplication getApplication() {
		return wizardModel.getApplication();
	}
}
