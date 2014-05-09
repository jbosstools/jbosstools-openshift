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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

/**
 * @author Andre Dietisheim
 */
public class WizardUtils {

	private WizardUtils() {
	}

	public static void close(IWizard wizard) {
		IWizardContainer container = wizard.getContainer();
		if (container instanceof WizardDialog) {
			((WizardDialog) container).close();
		}
	}
	
	public static boolean openWizard(int width, int height, IWizard wizard, Shell shell) {
		WizardDialog dialog = createWizardDialog(wizard, shell);
		dialog.setMinimumPageSize(width, height);
		return dialog.open() == Dialog.OK;
	}

	public static boolean openWizard(IWizard wizard, Shell shell) {
		WizardDialog dialog = createWizardDialog(wizard, shell);
		return dialog.open() == Dialog.OK;
	}

	private static WizardDialog createWizardDialog(IWizard wizard, Shell shell) {
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.create();
		return dialog;
	}

	public static boolean openWizard(IWorkbenchWizard wizard, Shell shell) {
		ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectionService.getSelection();
		if (selection instanceof IStructuredSelection) {
			return openWizard(wizard, shell, (IStructuredSelection) selection);
		} else {
			return openWizard(wizard, shell, null);
		}
	}

	public static boolean openWizard(IWorkbenchWizard wizard, Shell shell, IStructuredSelection selection) {
		WizardDialog wizardDialog = createWizardDialog(wizard, shell);
		wizard.init(PlatformUI.getWorkbench(), selection);
		return wizardDialog.open() == Dialog.OK;
	}
}
