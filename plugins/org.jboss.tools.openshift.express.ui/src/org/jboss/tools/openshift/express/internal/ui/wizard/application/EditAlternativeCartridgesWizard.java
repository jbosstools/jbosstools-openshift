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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.AbstractCheckEmbeddableCartridgeStrategy;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbeddedCartridgesWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbeddedCartridgesWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbeddedCartridgesWizardPageModel;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author André Dietisheim
 */
public class EditAlternativeCartridgesWizard extends AbstractOpenShiftWizard<EmbeddedCartridgesWizardModel> {

	private EmbeddedCartridgesWizardPage editAlternativeCartridgesWizardPage;

	public EditAlternativeCartridgesWizard(ICartridge selectedAlternative, List<ICartridge> alternativeCartridges, IOpenShiftApplicationWizardModel wizardModel) {
		super("Add Embedded Cartridges", 
				new EmbeddedCartridgesWizardModel(
						Collections.<ICartridge> singleton(selectedAlternative)
						, alternativeCartridges
						, new NewApplicationProperties(wizardModel)
						, wizardModel.getDomain()
						, wizardModel.getConnection()));
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(this.editAlternativeCartridgesWizardPage = new EditAlternativeCartridgesWizardPage(getModel(), this));
	}

	public ICartridge getCheckedCartridge() {
		Iterator<ICartridge> it = editAlternativeCartridgesWizardPage.getCheckedCartridges().iterator();
		if (!it.hasNext()) {
			return null;
		}
		return it.next();
	}
	
	protected static class EditAlternativeCartridgesWizardPage extends EmbeddedCartridgesWizardPage {

		EditAlternativeCartridgesWizardPage(EmbeddedCartridgesWizardModel wizardModel, IWizard wizard) {
			super("Select your cartridge",
					"Please select the cartridge that you want to add to your application.",
					wizardModel, wizard);
		}
		
		@Override
		protected ICheckStateListener onCartridgeChecked(EmbeddedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
			return new SelectAlternativeStrategy(pageModel, wizardPage);
		}

		@Override
		protected void createButtons(Composite parent, DataBindingContext dbc) {
			// no buttons
		}
	}
	
	private static class SelectAlternativeStrategy extends AbstractCheckEmbeddableCartridgeStrategy {

		SelectAlternativeStrategy(EmbeddedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
			super(pageModel, wizardPage);
		}

		@Override
		protected void add(ICartridge cartridge, CheckStateChangedEvent event) {
			uncheckAllBut(cartridge);
		}

		private void uncheckAllBut(ICartridge cartridge) {
			getPageModel().uncheckAll();
			getPageModel().checkEmbeddedCartridge(cartridge);
		}
		
		@Override
		protected void remove(ICartridge cartridge, CheckStateChangedEvent event) {			
			// undo visual change only
			event.getCheckable().setChecked(cartridge, true);
		}
	}
}
