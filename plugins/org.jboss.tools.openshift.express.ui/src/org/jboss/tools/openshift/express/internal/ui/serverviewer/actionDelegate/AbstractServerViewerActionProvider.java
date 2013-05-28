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

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.cnf.ServerActionProvider;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractOpenShiftAction;

/**
 * @author Xavier Coulon
 */
@SuppressWarnings("restriction")
public abstract class AbstractServerViewerActionProvider extends CommonActionProvider {

	private static final String OPENSHIFT_SERVER_ADAPTER_MENU = "org.jboss.tools.openshift.express.serverviewer.menu";

	protected final AbstractOpenShiftAction action;

	protected ICommonActionExtensionSite actionExtensionSite;

	public AbstractServerViewerActionProvider(AbstractOpenShiftAction action) {
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
				if (action != null) {
					MenuManager openshiftMenu = getOpenShiftMenuManager(menu);
					openshiftMenu.add(action);
				}
			}
		}
	}

	/**
	 * @param menu
	 * @return
	 */
	private MenuManager getOpenShiftMenuManager(IMenuManager menu) {
		for(IContributionItem item : menu.getItems()) {
			// make this call in this way, since item id can be null
			if(OPENSHIFT_SERVER_ADAPTER_MENU.equals(item.getId())) {
				return (MenuManager) item;
			}
		}
		MenuManager openshiftMenu = new MenuManager("OpenShift",
				OPENSHIFT_SERVER_ADAPTER_MENU);
		menu.add(openshiftMenu);
		menu.insertBefore(ServerActionProvider.TOP_SECTION_END_SEPARATOR, openshiftMenu);
		return openshiftMenu;
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