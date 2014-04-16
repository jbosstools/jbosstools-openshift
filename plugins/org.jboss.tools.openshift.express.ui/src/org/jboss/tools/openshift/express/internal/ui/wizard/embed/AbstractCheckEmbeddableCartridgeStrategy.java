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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;

import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * A strategy base class that allows custom behaviour when checking/unchecking
 * embeddable cartridges in a checkbox table. Implementors may provide behaviors
 * that also check/uncheck requirements etc. etc.)
 * 
 * @author Andre Dietisheim
 * 
 * @see EmbeddedCartridgesWizardPage
 * @see ICheckStateListener
 * @see CheckboxTableViewer
 * @see IEmbeddableCartridge
 */
public abstract class AbstractCheckEmbeddableCartridgeStrategy implements ICheckStateListener {

	private EmbeddedCartridgesWizardPageModel pageModel;
	private IWizardPage wizardPage;

	protected AbstractCheckEmbeddableCartridgeStrategy(EmbeddedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
		this.pageModel = pageModel;
		this.wizardPage = wizardPage;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		Assert.isLegal(event.getElement() instanceof ICartridge);
		
		ICartridge cartridge = (ICartridge) event.getElement();
		if(event.getChecked()) {
			add(cartridge, event);
		} else {
			remove(cartridge, event);
		}
	}
	
	protected abstract void add(ICartridge cartridge, CheckStateChangedEvent event);
	
	protected abstract void remove(ICartridge cartridge, CheckStateChangedEvent event);
	
	protected int openQuestionDialog(String title, String message) {
		return new MessageDialog(getShell(),
				title,
				null,
				message,
				MessageDialog.QUESTION,
				new String[] { "No", "Yes" }, 0)
				.open();
	}

	protected EmbeddedCartridgesWizardPageModel getPageModel() {
		return pageModel;
	}
	
	protected Shell getShell() {
		return wizardPage.getControl().getShell();
	}

	protected IWizardContainer getContainer() {
		return wizardPage.getWizard().getContainer();
	}
}
