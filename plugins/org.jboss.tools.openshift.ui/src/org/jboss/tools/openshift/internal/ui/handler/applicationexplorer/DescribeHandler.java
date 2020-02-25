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
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ServiceElement;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 */
public class DescribeHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement component = UIUtils.getFirstElement(selection, ComponentElement.class);
		ServiceElement service = null;
		ApplicationElement application = null;
		if (component == null) {
			service = UIUtils.getFirstElement(selection, ServiceElement.class);
			if (service == null) {
				application = UIUtils.getFirstElement(selection, ApplicationElement.class);
				if (application == null) {
					return OpenShiftUIActivator.statusFactory()
					        .cancelStatus("No application, component or service selected"); //$NON-NLS-1$
				}
			}
		}
		try {
			Odo odo = component!=null?component.getRoot().getOdo():service!=null?service.getRoot().getOdo():application.getRoot().getOdo();
			OpenShiftClient client = component!=null?component.getRoot().getClient():service!=null?service.getRoot().getClient():application.getRoot().getClient();
			final ServiceElement fService = service;
			final ApplicationElement fApplication = application;
			executeInJob("Describe", monitor -> execute(odo, client, component, fService, fApplication));
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void execute(Odo odo, OpenShiftClient client, ComponentElement component, ServiceElement service, ApplicationElement application) {
		try {
			if (component != null) {
				odo.describeComponent(component.getParent().getParent().getWrapped().getMetadata().getName(), component.getParent().getWrapped().getName(), component.getWrapped().getPath(), component.getWrapped().getName());
			} else if (service != null) {
				String template = odo.getServiceTemplate(client, service.getParent().getParent().getWrapped().getMetadata().getName(), service.getParent().getWrapped().getName(), service.getWrapped().getMetadata().getName());
				odo.describeServiceTemplate(template);
			} else {
				odo.describeApplication(application.getParent().getWrapped().getMetadata().getName(), application.getWrapped().getName());
			}
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
			        "Describe", "Describe error message:" + e.getLocalizedMessage()));
		}
	}
}
