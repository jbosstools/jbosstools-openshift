/*******************************************************************************
 * Copyright (c) 2013-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class EditConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IConnection connection = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IConnection.class);
		return openConnectionWizard(connection, event);
	}

	protected Object openConnectionWizard(IConnection connection, ExecutionEvent event) {
		final IWizard connectToOpenShiftWizard = new ConnectionWizard(connection, 
				ConnectionWizard.EDIT_CONNECTION_TITLE);
		WizardUtils.openWizardDialog(connectToOpenShiftWizard, HandlerUtil.getActiveShell(event));
		return Status.OK_STATUS;
	}
}
