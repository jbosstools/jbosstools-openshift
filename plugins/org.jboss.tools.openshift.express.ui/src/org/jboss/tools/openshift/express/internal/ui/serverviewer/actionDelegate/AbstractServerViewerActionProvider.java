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
package org.jboss.tools.openshift.express.internal.ui.serverviewer.actionDelegate;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractAction;

/**
 * @author Xavier Coulon
 */
public abstract class AbstractServerViewerActionProvider extends CommonActionProvider {

	protected final AbstractAction action;

	protected ICommonActionExtensionSite actionExtensionSite;

	public AbstractServerViewerActionProvider(AbstractAction action) {
		this.action = action;
	}

	public void init(ICommonActionExtensionSite actionExtensionSite) {
		super.init(actionExtensionSite);
		this.actionExtensionSite = actionExtensionSite;
		ICommonViewerSite site = actionExtensionSite.getViewSite();
		if (site instanceof ICommonViewerWorkbenchSite) {
			action.setViewer(actionExtensionSite.getStructuredViewer());
			action.setSelection(actionExtensionSite.getStructuredViewer().getSelection());
			actionExtensionSite.getStructuredViewer().addSelectionChangedListener(action);
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		Object sel = getSelection();
		if (sel instanceof IServer) {
			IServer server = (IServer) sel;
			if (ExpressServerUtils.isOpenShiftRuntime(server) || ExpressServerUtils.isInOpenshiftBehaviourMode(server)) {
				action.validate();
				if (action != null/* && action.isEnabled() */) {
					MenuManager openshiftMenu = new MenuManager("Openshift...",
							"org.jboss.tools.openshift.express.serverviewer.menu");
					openshiftMenu.add(action);
					menu.add(openshiftMenu);
				}
			}
		}
	}

	protected Object getSelection() {
		ICommonViewerSite site = actionExtensionSite.getViewSite();
		IStructuredSelection selection = null;
		if (site instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
			selection = (IStructuredSelection) wsSite.getSelectionProvider().getSelection();
			Object first = selection.getFirstElement();
			return first;
		}
		return null;
	}

}