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
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerView;

/**
 * @author Xavier Coulon
 */
public class RefreshViewerActionDelegate implements IViewActionDelegate {

	private OpenShiftExplorerView view;

	protected ISelection selection;
	
	@Override
	public void run(IAction action) {
		view.refreshViewer();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void init(IViewPart view) {
		if (view instanceof OpenShiftExplorerView) {
			this.view = (OpenShiftExplorerView) view;
		}
	}

}
