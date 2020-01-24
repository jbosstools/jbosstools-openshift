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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateURLModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateURLWizard;

/**
 * @author Red Hat Developers
 */
public class CreateURLHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement component = UIUtils.getFirstElement(selection, ComponentElement.class);
		if (component == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No project selected"); //$NON-NLS-1$
		}
		try {
			Odo odo = component.getRoot().getOdo();
			String projectName = component.getParent().getParent().getWrapped().getMetadata().getName();
			String applicationName = component.getParent().getWrapped().getName();
			List<Integer> ports = odo.getServicePorts(component.getRoot().getClient(), projectName, applicationName,
			        component.getWrapped().getName());
			if (ports.isEmpty()) {
				MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "Create url",
				        "No ports defined for this components to bind to.");
			} else {
				final CreateURLModel model = new CreateURLModel(odo, projectName, applicationName,
				        component.getWrapped().getName(), ports);
				final IWizard createServiceWizard = new CreateURLWizard(model);
				if (WizardUtils.openWizardDialog(createServiceWizard, HandlerUtil.getActiveShell(event)) == Window.OK) {
					executeInJob("Create url", () -> execute(model, component));
				}
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}
	
	private void execute(CreateURLModel model, ComponentElement component) {
		try {
			model.getOdo().createURL(model.getProjectName(), model.getApplicationName(), component.getWrapped().getPath(), model.getComponentName(), model.getURLName(), model.getPort());
			component.refresh();
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), "Create url", "Can't create url error message:" + e.getLocalizedMessage()));
		}
	}


}
