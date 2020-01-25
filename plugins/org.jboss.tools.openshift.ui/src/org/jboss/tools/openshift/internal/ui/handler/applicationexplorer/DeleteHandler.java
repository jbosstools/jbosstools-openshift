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
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ProjectElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ServiceElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.StorageElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.URLElement;

import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 */
public class DeleteHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		AbstractOpenshiftUIElement<?, ?, ?> element = UIUtils.getFirstElement(selection, AbstractOpenshiftUIElement.class);
		if (element == null) {
					return OpenShiftUIActivator.statusFactory()
					        .cancelStatus("No element selected"); //$NON-NLS-1$
		}
		try {
			String label = getLabel(element);
			if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), String.join(" ", "Delete", label), String.join(" ", "Are you sure to delete", label, "?"))) {
				Odo odo = ((ApplicationExplorerUIModel)element.getRoot()).getOdo();
				OpenShiftClient client = ((ApplicationExplorerUIModel)element.getRoot()).getClient();
				executeInJob("Delete", () -> execute(odo, client, element));
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	/**
	 * @param element
	 * @return
	 */
	private String getLabel(AbstractOpenshiftUIElement<?, ?, ?> element) {
		if (element instanceof ProjectElement) {
			return String.join(" ", "project", ((ProjectElement)element).getWrapped().getMetadata().getName());
		} else if (element instanceof ApplicationElement) {
			return String.join("", "application", ((ApplicationElement)element).getWrapped().getName());
		} else if (element instanceof ComponentElement) {
			return String.join(" ", "component", ((ComponentElement)element).getWrapped().getName());
		} else if (element instanceof ServiceElement) {
			return String.join("", "service", ((ServiceElement)element).getWrapped().getMetadata().getName());
		} else if (element instanceof URLElement) {
			return String.join(" ","url", ((URLElement)element).getWrapped().getName());
		} else if (element instanceof StorageElement) {
			return String.join(" ", "storage", ((StorageElement)element).getWrapped().getName());
		}
		return "";
	}

	private void execute(Odo odo, OpenShiftClient client, AbstractOpenshiftUIElement<?, ?, ?> element) {
		try {
			if (element instanceof ProjectElement) {
				odo.deleteProject(((ProjectElement) element).getWrapped().getMetadata().getName());
			} else if (element instanceof ApplicationElement) {
				odo.deleteApplication(client,
				        ((ApplicationElement) element).getParent().getWrapped().getMetadata().getName(),
				        ((ApplicationElement) element).getWrapped().getName());
			} else if (element instanceof ComponentElement) {
				odo.deleteComponent(
				        ((ComponentElement) element).getParent().getParent().getWrapped().getMetadata().getName(),
				        ((ComponentElement) element).getParent().getWrapped().getName(),
				        ((ComponentElement) element).getWrapped().getPath(),
				        ((ComponentElement) element).getWrapped().getName(),
				        ((ComponentElement) element).getWrapped().getState() != ComponentState.NOT_PUSHED);
			} else if (element instanceof ServiceElement) {
				odo.deleteService(
				        ((ServiceElement) element).getParent().getParent().getWrapped().getMetadata().getName(),
				        ((ServiceElement) element).getParent().getWrapped().getName(),
				        ((ServiceElement) element).getWrapped().getMetadata().getName());
			} else if (element instanceof URLElement) {
				odo.deleteURL(
				        ((URLElement) element).getParent().getParent().getParent().getWrapped().getMetadata().getName(),
				        ((URLElement) element).getParent().getParent().getWrapped().getName(),
				        ((URLElement) element).getParent().getWrapped().getPath(),
				        ((URLElement) element).getParent().getWrapped().getName(),
				        ((URLElement) element).getWrapped().getName());
			} else if (element instanceof StorageElement) {
				odo.deleteStorage(
				        ((StorageElement) element).getParent().getParent().getParent().getWrapped().getMetadata()
				                .getName(),
				        ((StorageElement) element).getParent().getParent().getWrapped().getName(),
				        ((StorageElement) element).getParent().getWrapped().getPath(),
				        ((StorageElement) element).getParent().getWrapped().getName(),
				        ((StorageElement) element).getWrapped().getName());
			}
			element.getParent().refresh();
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
			        "Delete", "Delete error:" + e.getLocalizedMessage()));
		}
	}

}
