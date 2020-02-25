/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LoginModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LoginWizard;

/**
 * @author Red Hat Developers
 */
public class LoginHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ApplicationExplorerUIModel cluster = UIUtils.getFirstElement(selection, ApplicationExplorerUIModel.class);
		if (cluster == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No cluster selected"); //$NON-NLS-1$
		}
		try {
			openDialog(HandlerUtil.getActiveShell(event), cluster);
			return Status.OK_STATUS;
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
	}

	public static void openDialog(final Shell shell, ApplicationExplorerUIModel cluster) throws IOException {
		final LoginModel model = new LoginModel(cluster.getClient().getMasterUrl().toString(), cluster.getOdo());
		final IWizard loginWizard = new LoginWizard(model);
		if (WizardUtils.openWizardDialog(loginWizard, shell) == Window.OK) {
			executeInJob("Login to Cluster", monitor -> execute(model));
		}
	}
	
	private static void execute(LoginModel model) {
		try {
			model.getOdo().login(model.getUrl(), model.getUsername(), model.getPassword().toCharArray(), model.getToken());
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), "Login", "Can't login error message:" + e.getLocalizedMessage()));
		}
	}
}
