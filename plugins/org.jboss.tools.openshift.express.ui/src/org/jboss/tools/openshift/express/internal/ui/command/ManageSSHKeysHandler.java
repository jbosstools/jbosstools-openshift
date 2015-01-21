/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.ManageSSHKeysWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class ManageSSHKeysHandler extends AbstractHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExpressConnection connection = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), ExpressConnection.class);
		if (connection != null) {
			new OkButtonWizardDialog(HandlerUtil.getActiveShell(event), new ManageSSHKeysWizard(connection)).open();
		}
		return null;
	}
}
