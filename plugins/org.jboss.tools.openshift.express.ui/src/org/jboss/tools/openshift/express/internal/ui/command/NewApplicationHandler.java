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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.NewOpenShiftApplicationWizard;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class NewApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDomain domain = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IDomain.class);
		if (domain != null) {
			open(new NewOpenShiftApplicationWizard(domain), HandlerUtil.getActiveShell(event));
		} else {
			Connection connection = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), Connection.class);
			open(new NewOpenShiftApplicationWizard(connection), HandlerUtil.getActiveShell(event));
		}

		return null;
	}

	private void open(IWorkbenchWizard wizard, Shell shell) {
		try {
			WizardUtils.openWizard(wizard, shell);
		} catch (NullPointerException e) {
			// swallow NPE that's caused by cancelling ssh keys / domain wizard
			// https://issues.jboss.org/browse/JBIDE-14575
		}

	}
}
