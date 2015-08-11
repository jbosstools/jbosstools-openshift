/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.portforwading.PortForwardingWizard;
import org.jboss.tools.openshift.internal.ui.portforwading.PortForwardingWizardModel;

import com.openshift.restclient.model.IPod;

/**
 * @author jeff.cantrill
 */
public class PortForwardingHandler extends AbstractOpenShiftCliHandler {

	@Override
	protected void handleEvent(ExecutionEvent event){
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		final IPod pod = UIUtils.getFirstElement(selection, IPod.class);
		if(pod == null) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "No pod selection", "No pod was selected for port forwarding.");
		}
		openDialog(pod);
	}

	private void openDialog(final IPod pod) {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		WizardDialog dialog = new OkButtonWizardDialog(shell, new PortForwardingWizard(new PortForwardingWizardModel(pod)));
		dialog.setMinimumPageSize(700, 400);
		dialog.create();
		dialog.open();
	}
}
