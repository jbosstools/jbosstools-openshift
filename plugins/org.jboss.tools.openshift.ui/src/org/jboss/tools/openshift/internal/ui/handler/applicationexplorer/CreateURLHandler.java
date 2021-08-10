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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.ComponentKind;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateURLModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateURLWizard;

/**
 * @author Red Hat Developers
 */
public class CreateURLHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			String projectName = component.getParent().getParent().getWrapped().getMetadata().getName();
			String applicationName = component.getParent().getWrapped().getName();
			List<Integer> ports = component.getWrapped().getInfo().getComponentKind() == ComponentKind.S2I ?odo.getServicePorts(projectName, applicationName,
					component.getWrapped().getName()) : Collections.emptyList();
			if (component.getWrapped().getInfo().getComponentKind() == ComponentKind.S2I && ports.isEmpty()) {
				MessageDialog.openWarning(shell, "Create url", "No ports defined for this components to bind to.");
			} else {
				final CreateURLModel model = new CreateURLModel(odo, projectName, applicationName,
						component.getWrapped().getName(), ports);
				final IWizard createURLWizard = new CreateURLWizard(model);
				if (WizardUtils.openWizardDialog(createURLWizard, shell) == Window.OK) {
					executeInJob("Create url", monitor -> execute(model, component));
				}
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(CreateURLModel model, ComponentElement component) {
		try {
			model.getOdo().createURL(model.getProjectName(), model.getApplicationName(),
					component.getWrapped().getPath(), model.getComponentName(), model.getURLName(), model.getPort(),
					model.isSecure());
			component.refresh();
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Create url", "Can't create url error message:" + e.getLocalizedMessage()));
		}
	}
}
