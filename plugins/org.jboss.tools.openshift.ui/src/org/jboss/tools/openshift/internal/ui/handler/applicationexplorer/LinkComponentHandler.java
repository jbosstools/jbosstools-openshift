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
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkComponentModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkComponentWizard;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 */
public class LinkComponentHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			OpenShiftClient client = component.getRoot().getClient();
			String projectName = component.getParent().getParent().getWrapped().getMetadata().getName();
			String applicationName = component.getParent().getWrapped().getName();
			List<Component> targetComponents = odo.getComponents(client, projectName, applicationName).stream().filter(comp -> !comp.getName().equals(component.getWrapped().getName())).collect(Collectors.toList());
			if (targetComponents.isEmpty()) {
				MessageDialog.openError(shell, "Link component", "No component available to link to.");
			} else {
				final LinkComponentModel model = new LinkComponentModel(odo, projectName, applicationName,
				        component.getWrapped().getName(), Collections.emptyList(), client);
				model.setTargets(targetComponents);
				final IWizard linkComponentWizard = new LinkComponentWizard(model);
				if (WizardUtils.openWizardDialog(linkComponentWizard, shell) == Window.OK) {
					Shell parent = Display.getDefault().getActiveShell();
					executeInJob("Link component", monitor -> execute(parent, model, component));
				}
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(Shell shell, LinkComponentModel model, ComponentElement component) {
		Notification notification = openNotification(shell, "Linking component " + model.getComponentName() + " to " + model.getTarget().getName());
		try {
			model.getOdo().link(model.getProjectName(), model.getApplicationName(), component.getWrapped().getName(),
			        component.getWrapped().getPath(), model.getTarget().getName(), model.getPort());
			openNotification(notification, shell, "Component " + model.getComponentName() + " linked to " + model.getTarget().getName());
		} catch (IOException e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(shell,
				        "Link component", "Can't link component error message:" + e.getLocalizedMessage());
			});
		}
	}
}
