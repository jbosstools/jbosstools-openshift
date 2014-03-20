/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.util.Set;

import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbeddedCartridgesWizardPage;

import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author André Dietisheim
 */
public class AddEmbeddableCartridgesWizard extends AbstractOpenShiftWizard<AddEmbeddableCartridgesWizardModel> {

	private EmbeddedCartridgesWizardPage embeddedCartridgesWizardPage;

	public AddEmbeddableCartridgesWizard(IOpenShiftApplicationWizardModel wizardModel) {
		super("Add Embedded Cartridges", new AddEmbeddableCartridgesWizardModel(wizardModel));
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(this.embeddedCartridgesWizardPage = new EmbeddedCartridgesWizardPage(getModel(), this));
	}

	public Set<IEmbeddableCartridge> getCheckedEmbeddableCartridges() {
		return embeddedCartridgesWizardPage.getCheckedEmbeddableCartridges();
	}
}
