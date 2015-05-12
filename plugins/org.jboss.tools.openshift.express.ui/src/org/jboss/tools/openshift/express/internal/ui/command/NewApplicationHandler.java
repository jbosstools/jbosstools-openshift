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
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.NewOpenShiftApplicationWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class NewApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDomain domain = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IDomain.class);
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (domain != null) {
			open(new NewOpenShiftApplicationWizard(domain), shell);
		} else {
			ExpressConnection connection = UIUtils.getFirstElement(selection, ExpressConnection.class);
			if (connection != null) {
				open(new NewOpenShiftApplicationWizard(connection), shell);
			} else {
				IProject project = UIUtils.getFirstElement(selection, IProject.class);
				open(new NewOpenShiftApplicationWizard(project), shell);
			}
		}

		return null;
	}

	private void open(IWizard wizard, Shell shell) {
		try {
			WizardUtils.openWizard(wizard, shell);
		} catch (NullPointerException e) {
			// swallow NPE that's caused by cancelling ssh keys / domain wizard
			// https://issues.jboss.org/browse/JBIDE-14575
		}

	}
}
