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
import org.jboss.tools.common.ui.notification.LabelNotification;
import org.jboss.tools.openshift.core.odo.KubernetesLabels;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkServiceWizard;

import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 */
public class LinkServiceHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			OpenShiftClient client = component.getRoot().getClient();
			String projectName = component.getParent().getParent().getWrapped().getMetadata().getName();
			String applicationName = component.getParent().getWrapped().getName();
			List<String> serviceNames = odo.getServices(client, projectName, applicationName).stream()
			        .map(service -> KubernetesLabels.getComponentName(service)).collect(Collectors.toList());
			final LinkModel<String> model = new LinkModel<>(odo, projectName, applicationName,
			        component.getWrapped().getName(), serviceNames);
			final IWizard linkServiceWizard = new LinkServiceWizard(model);
			if (WizardUtils.openWizardDialog(linkServiceWizard, shell) == Window.OK) {
				Shell parent = Display.getDefault().getActiveShell();
				executeInJob("Link service", monitor -> execute(parent, model, component));
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(Shell shell, LinkModel<String> model, ComponentElement component) {
		LabelNotification notification = LabelNotification.openNotification(shell,
		        "Linking component " + model.getComponentName() + " to service " + model.getTarget());
		try {
			model.getOdo().link(model.getProjectName(), model.getApplicationName(), component.getWrapped().getName(),
			        component.getWrapped().getPath(), model.getTarget(), null);
			LabelNotification.openNotification(notification, shell,
			        "Component " + model.getComponentName() + " linked to service " + model.getTarget());
		} catch (IOException e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Link service",
				        "Can't link service error message:" + e.getLocalizedMessage());
			});
		}
	}
}
