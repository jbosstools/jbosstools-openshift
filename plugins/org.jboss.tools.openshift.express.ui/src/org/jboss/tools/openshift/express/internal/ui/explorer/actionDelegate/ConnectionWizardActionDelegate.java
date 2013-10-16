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
package org.jboss.tools.openshift.express.internal.ui.explorer.actionDelegate;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizard;

/**
 * @author Xavier Coulon
 */
public class ConnectionWizardActionDelegate implements IViewActionDelegate {

	private Connection selectedConnection;

	@Override
	public void run(IAction action) {
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final IWizard connectToOpenShiftWizard = new ConnectionWizard(selectedConnection);
		int returnCode = WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
		if (returnCode == Window.CANCEL) {
			return;
		}
		Logger.debug("OpenShift Auth succeeded.");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selectedConnection = UIUtils.getFirstElement(selection, Connection.class);
	}
	
	@Override
	public void init(IViewPart view) {
	}

}