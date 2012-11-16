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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectToOpenShiftWizard;

/**
 * @author Xavier Coulon
 */
public class OpenConnectionDialogActionDelegate implements IViewActionDelegate {

	private CommonNavigator view;
	private Connection selectedConnection;

	@Override
	public void run(IAction action) {
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final IWizard connectToOpenShiftWizard = new ConnectToOpenShiftWizard(selectedConnection);
		int returnCode = WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
		if (returnCode == Window.OK) {
			Logger.debug("OpenShift Auth succeeded.");
			if (view != null) {
				view.getCommonViewer().setInput(ConnectionsModelSingleton.getInstance());
			}
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedConnection = getSelectedConnection(selection);
	}

	private Connection getSelectedConnection(ISelection selection) {
		Connection selectedConnection = null;
		if (selection instanceof IStructuredSelection) {
			Object selectedItem = ((IStructuredSelection) selection).getFirstElement();
			if (selectedItem instanceof Connection) {
				selectedConnection = (Connection) selectedItem;
			}
		}
		return selectedConnection;
	}
	
	@Override
	public void init(IViewPart view) {
		if (view instanceof CommonNavigator) {
			this.view = (CommonNavigator) view;
		}
	}

}
