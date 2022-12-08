/*******************************************************************************
 * Copyright (c) 2021-2022 Red Hat, Inc.
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistriesElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateDevfileRegistryModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateDevfileRegistryWizard;

/**
 * @author Red Hat Developers
 */
public class CreateDevfileRegistryHandler extends OdoJobHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		DevfileRegistriesElement registriesElement = UIUtils.getFirstElement(selection, DevfileRegistriesElement.class);
		final CreateDevfileRegistryModel model = new CreateDevfileRegistryModel(registriesElement.getRoot().getOdo());
		final IWizard wizard = new CreateDevfileRegistryWizard(model);
		if (WizardUtils.openWizardDialog(wizard, HandlerUtil.getActiveShell(event)) == Window.OK) {
			executeInJob("Create devfile registry", monitor -> execute(model, registriesElement));
		}
		return null;
	}

	private void execute(CreateDevfileRegistryModel model, DevfileRegistriesElement registriesElement) {
		try {
			model.getOdo().createDevfileRegistry(model.getName(), model.getURL(), model.getToken());
			registriesElement.refresh();
		} catch (IOException e) {
			Display.getDefault()
					.asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Create devfile registry",
							"Can't create devfile registry. \n Error message: " + e.getLocalizedMessage()));
		}
	}
}
