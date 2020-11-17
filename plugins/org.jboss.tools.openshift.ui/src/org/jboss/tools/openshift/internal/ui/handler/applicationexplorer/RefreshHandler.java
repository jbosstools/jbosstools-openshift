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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
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
public class RefreshHandler extends OdoHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ApplicationExplorerUIModel cluster = UIUtils.getFirstElement(selection, ApplicationExplorerUIModel.class);
		if (cluster == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No cluster selected"); //$NON-NLS-1$
		}
		Shell parent = HandlerUtil.getActiveShell(event);
		executeInJob("Refresh cluster", monitor -> execute(parent, cluster));
		return null;
	}

	/**
	 * @param cluster
	 * @param value
	 * @return
	 */
	private void execute(Shell shell, ApplicationExplorerUIModel cluster) {
		LabelNotification notification = LabelNotification.openNotification(shell, "Refresh cluster");
		try {
			cluster.refresh();
			LabelNotification.openNotification(notification, shell, "Cluster refreshed");
		} catch (Exception e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(shell, "Error during refresh", "Cannot refresh cluster");
			});
		}
	}

}
