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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ApplicationWizardModel;

import com.openshift.client.IApplication;

/**
 * @author André Dietisheim
 */
public class EmbedCartridgeWizard extends Wizard {

	private ApplicationWizardModel wizardModel;
	private EmbedCartridgeWizardPage embedCartridgeWizardPage;

	public EmbedCartridgeWizard(IApplication application, Connection connection) {
		this.wizardModel = new ApplicationWizardModel(application, connection);
		setNeedsProgressMonitor(true);
		setWindowTitle("Edit Embedded Cartridges");
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
