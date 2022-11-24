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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateServiceWizard;

/**
 * @author Red Hat Developers
 */
public class CreateServiceHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		NamespaceElement project = UIUtils.getFirstElement(selection, NamespaceElement.class);
		try {
			Odo odo = project.getParent().getOdo();
			List<ServiceTemplate> templates = odo.getServiceTemplates();
			if (!templates.isEmpty()) {
				final IWizard createServiceWizard = new CreateServiceWizard(templates, project.getWrapped(), odo);
				if (WizardUtils.openWizardDialog(createServiceWizard, HandlerUtil.getActiveShell(event)) == Window.OK) {
					project.refresh();
				}
			} else {
				MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "Create service",
						"No operators installed on your cluster, can't create services");
			}
			return Status.OK_STATUS;

		} catch (IOException e) {
			String title = "Unable to create service";
			String message = e.getMessage();
			MessageDialog.open(MessageDialog.ERROR, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title, message, SWT.NONE);
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

}
