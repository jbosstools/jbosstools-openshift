/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.notification.LabelNotification;
import org.jboss.tools.openshift.core.odo.Binding;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.Service;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.LinkServiceWizard;

/**
 * @author Red Hat Developers
 */
public class LinkServiceHandler extends ComponentHandler {

	@Override
	public Object execute(ComponentElement component, Shell shell) throws ExecutionException {
		try {
			Odo odo = component.getRoot().getOdo();
			String projectName = component.getParent().getWrapped();
			List<Service> serviceNames = odo.getServices(projectName);
			final LinkModel<Service> model = new LinkModel<>(odo, projectName, component.getWrapped().getName(),
					serviceNames);
			final IWizard linkServiceWizard = new LinkServiceWizard(model);
			if (WizardUtils.openWizardDialog(linkServiceWizard, shell) == Window.OK) {
				executeInJob("Link service", monitor -> execute(shell, model, component));
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(Shell shell, LinkModel<Service> model, ComponentElement component) {
		LabelNotification notification = LabelNotification.openNotification(shell,
				"Linking component " + model.getComponentName() + " to service " + model.getTarget().getName());
		try {
			Service service = model.getTarget();
			String target = service.getName() + '/' + service.getKind() + "." + service.getApiVersion();
			Binding binding = model.getOdo().link(model.getProjectName(), component.getWrapped().getPath(),
					component.getWrapped().getName(), target);
			/*
			 * if (!binding.getEnvironmentVariables().isEmpty()) {
			 * ApplicationManager.getApplication().invokeLater(() -> { var dialog = new
			 * BindingDetailDialog(anActionEvent.getProject(), null, binding);
			 * dialog.show(); });
			 * 
			 * } } else {
			 */
			LabelNotification.openNotification(notification, shell,
					"Component " + model.getComponentName() + " linked to service " + model.getTarget().getName());
		} catch (IOException e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Link service",
						"Can't link service error message:" + e.getLocalizedMessage());
			});
		}
	}
}