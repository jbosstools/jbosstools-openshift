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
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.explorer.OpenShiftExplorerView;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.ConnectionWrapper;

/**
 * @author Andre Dietisheim
 */
public class EditConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    ConnectionWrapper connectionWrapper = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), ConnectionWrapper.class);
	    int expandToLevel = findExpansionLevel(connectionWrapper);
        IConnection connection = connectionWrapper.getWrapped();
	    IStatus status = openConnectionWizard(connection, event);
        if (Status.OK_STATUS.equals(status)) {
		    connectionWrapper.refresh();
		    getTreeViewer().expandToLevel(connectionWrapper, expandToLevel);
		}
		return null;
	}

	private int findExpansionLevel(ConnectionWrapper connectionWrapper) {
        TreePath[] expandedTreePaths = getTreeViewer().getExpandedTreePaths();
        return Arrays.stream(expandedTreePaths)
                .filter(tp -> connectionWrapper.equals(tp.getFirstSegment()))
                .map(TreePath::getSegmentCount)
                .reduce(Integer.MIN_VALUE, Integer::max);
	}
	
	private TreeViewer getTreeViewer() {
	    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        OpenShiftExplorerView explorerView = (OpenShiftExplorerView)page.findView("org.jboss.tools.openshift.express.ui.explorer.expressConsoleView");
        return explorerView.getCommonViewer();
	}

	protected IStatus openConnectionWizard(IConnection connection, ExecutionEvent event) {
		final IWizard connectToOpenShiftWizard = new ConnectionWizard(connection,
				ConnectionWizard.EDIT_CONNECTION_TITLE);
		WizardUtils.openWizardDialog(connectToOpenShiftWizard, HandlerUtil.getActiveShell(event));
		return Status.OK_STATUS;
	}
}
