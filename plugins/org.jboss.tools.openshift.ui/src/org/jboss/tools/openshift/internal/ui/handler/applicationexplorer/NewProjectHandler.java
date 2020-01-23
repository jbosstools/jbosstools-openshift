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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;

/**
 * @author Red Hat Developers
 */
public class NewProjectHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ApplicationExplorerUIModel cluster = UIUtils.getFirstElement(selection, ApplicationExplorerUIModel.class);
		if (cluster == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No cluster selected"); //$NON-NLS-1$
		}
		Shell shell = HandlerUtil.getActiveShell(event);
		InputDialog dialog = new InputDialog(shell, "New project", "Project name:", null, null);
		try {
			if (dialog.open() == Window.OK) {
				cluster.getOdo().createProject(dialog.getValue());
				cluster.refresh();
			}
		} catch (IOException e) {
			throw new ExecutionException(e.getLocalizedMessage(), e);
		}
		return null;
	}


}
