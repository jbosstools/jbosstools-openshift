/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.io.core.TokenProvider;
import org.jboss.tools.openshift.io.internal.ui.OpenShiftIOUIActivator;

public class GetTokenHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			TokenProvider provider = TokenProvider.get();
			String token = provider.getToken(null);
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "OpenShift.io", "Token retrieved is:" + token.substring(0, 16));
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, OpenShiftIOUIActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
			ErrorDialog dialog = new ErrorDialog(HandlerUtil.getActiveShell(event), "OpenShift.io", e.getLocalizedMessage(), status, IStatus.ERROR);
			dialog.open();
		}
		return null;
	}
}
