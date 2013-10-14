/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizard;

/**
 * @author Andre Dietisheim
 */
public class EditConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Connection connection = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), Connection.class);
		return openConnectionWizard(connection, event);
	}

	protected Object openConnectionWizard(Connection connection, ExecutionEvent event) {
		final IWizard connectToOpenShiftWizard = new ConnectionWizard(connection);
		WizardUtils.openWizardDialog(connectToOpenShiftWizard, HandlerUtil.getActiveShell(event));
		return null;
	}
}
