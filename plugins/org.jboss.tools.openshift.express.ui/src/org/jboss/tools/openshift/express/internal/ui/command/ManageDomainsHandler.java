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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.job.FireExpressConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.ManageDomainsWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class ManageDomainsHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		final ExpressConnection connection = UIUtils.getFirstElement(selection, ExpressConnection.class);
		if (connection == null) {
			return Status.OK_STATUS;
		}

		new OkButtonWizardDialog(HandlerUtil.getActiveShell(event), 
				new ManageDomainsWizard("Domains", 
						NLS.bind("Manage your domains for connection {0}", connection.getId()), connection)).open();
		new FireExpressConnectionsChangedJob(connection).schedule();
		return Status.OK_STATUS;
	}
}
