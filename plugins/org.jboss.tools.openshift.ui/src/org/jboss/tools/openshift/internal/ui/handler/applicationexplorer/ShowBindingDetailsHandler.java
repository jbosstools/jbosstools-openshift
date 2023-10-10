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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.dialog.applicationexplorer.BindingDetailDialog;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.BindingElement;

/**
 * @author Red Hat Developers
 */
public class ShowBindingDetailsHandler extends OdoJobHandler {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		BindingElement binding = UIUtils.getFirstElement(selection, BindingElement.class);

		// try {
		if (binding != null) {
			BindingDetailDialog dialog = new BindingDetailDialog(HandlerUtil.getActiveShell(event), binding.getWrapped());
			dialog.open();
			UsageStats.getInstance().odoCommand("describe binding", true);

		}
		// } catch (IOException e) {
		// Display.getDefault().asyncExec(() ->
		// MessageDialog.openError(Display.getDefault().getActiveShell(),
		// "Describe", "Describe error message:" + e.getLocalizedMessage()));
		UsageStats.getInstance().odoCommand("describe binding", false);
		// }
		return Status.OK_STATUS;
	}

}
