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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.Storage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateStorageModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateStorageWizard;

/**
 * @author Red Hat Developers
 */
public class CreateStorageHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			String projectName = component.getParent().getParent().getWrapped().getMetadata().getName();
			String applicationName = component.getParent().getWrapped().getName();
				final CreateStorageModel model = new CreateStorageModel(odo, projectName, applicationName,
				        component.getWrapped().getName(), Storage.getSizes());
				final IWizard createStorageWizard = new CreateStorageWizard(model);
				if (WizardUtils.openWizardDialog(createStorageWizard, shell) == Window.OK) {
					executeInJob("Create storage", () -> execute(model, component));
				}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}
	
	private void execute(CreateStorageModel model, ComponentElement component) {
		try {
			model.getOdo().createStorage(model.getProjectName(), model.getApplicationName(), component.getWrapped().getPath(), model.getComponentName(), model.getStorageName(), model.getMountPath(), model.getSize());
			component.refresh();
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), "Create storage", "Can't create storage error message:" + e.getLocalizedMessage()));
		}
	}


}
