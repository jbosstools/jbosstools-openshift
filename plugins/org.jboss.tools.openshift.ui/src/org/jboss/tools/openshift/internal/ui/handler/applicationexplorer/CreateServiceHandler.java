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
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ProjectElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateServiceWizard;

/**
 * @author Red Hat Developers
 */
public class CreateServiceHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ApplicationElement application = null;
		ProjectElement project = UIUtils.getFirstElement(selection, ProjectElement.class);
		if (project == null) {
			application = UIUtils.getFirstElement(selection, ApplicationElement.class);
			if (application == null) {
				return OpenShiftUIActivator.statusFactory().cancelStatus("No project or application selected"); //$NON-NLS-1$
			}
			project = application.getParent();
		}
		try {
			Odo odo = project.getParent().getOdo();
			final IWizard createServiceWizard = new CreateServiceWizard(odo.getServiceTemplates(), project.getWrapped().getMetadata().getName(), application==null?"":application.getWrapped().getName(), odo);
			WizardUtils.openWizardDialog(createServiceWizard, HandlerUtil.getActiveShell(event));
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}


}
