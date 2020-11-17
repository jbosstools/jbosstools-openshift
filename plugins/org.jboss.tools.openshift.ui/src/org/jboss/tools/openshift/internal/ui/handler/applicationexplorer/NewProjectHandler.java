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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.notification.LabelNotification;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;

/**
 * @author Red Hat Developers
 */
public class NewProjectHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ApplicationExplorerUIModel cluster = UIUtils.getFirstElement(selection, ApplicationExplorerUIModel.class);
		if (cluster == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No cluster selected"); //$NON-NLS-1$
		}
		Shell shell = HandlerUtil.getActiveShell(event);
		InputDialog dialog = new InputDialog(shell, "New project", "Project name:", null, null);
		if (dialog.open() == Window.OK) {
			executeInJob("Create project", monitor -> execute(shell, cluster, dialog.getValue()));
		}
		return null;
	}

	/**
	 * @param cluster
	 * @param value
	 * @return
	 */
	private void execute(Shell shell, ApplicationExplorerUIModel cluster, String project) {
		LabelNotification notification = LabelNotification.openNotification(shell, "Creating project " + project);
		try {
			cluster.getOdo().createProject(project);
			cluster.refresh();
			LabelNotification.openNotification(notification, shell, "Project " + project + " created");
		} catch (IOException e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(shell, "Create project", "Error creating project " + project);
			});
		}
	}

}
