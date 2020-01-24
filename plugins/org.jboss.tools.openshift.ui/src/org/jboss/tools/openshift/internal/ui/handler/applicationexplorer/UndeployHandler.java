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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.odo.ComponentState;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

/**
 * @author Red Hat Developers
 */
public class UndeployHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement component = UIUtils.getFirstElement(selection, ComponentElement.class);
		if (component == null) {
					return OpenShiftUIActivator.statusFactory()
					        .cancelStatus("No component selected"); //$NON-NLS-1$
		}
		try {
			Odo odo = component.getRoot().getOdo();
			executeInJob("Undeploy", () -> execute(odo, component));
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(Odo odo, ComponentElement component) {
		try {
			odo.undeployComponent(component.getParent().getParent().getWrapped().getMetadata().getName(),
			        component.getParent().getWrapped().getName(), component.getWrapped().getPath(),
			        component.getWrapped().getName());
			component.getWrapped().setState(ComponentState.NOT_PUSHED);
			component.refresh();
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
			        "Undeploy", "Undeploy error:" + e.getLocalizedMessage()));
		}
	}

}
