/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

public class WizardFragmentUtils {

	public static WizardPage getWizardPage(IWizardHandle handle) {
		return (WizardPage) handle;
	}

	public static IWizard getWizard(IWizardHandle handle) {
		IWizardPage page = getWizardPage(handle);
		if (page == null) {
			return null;
		}
		return page.getWizard();
	}

	public static WizardDialog getWizardDialog(IWizardHandle handle) {
		IWizard wizard = getWizard(handle);
		if (wizard == null) {
			return null;
		}
		return (WizardDialog) wizard.getContainer();
	}
	
}
