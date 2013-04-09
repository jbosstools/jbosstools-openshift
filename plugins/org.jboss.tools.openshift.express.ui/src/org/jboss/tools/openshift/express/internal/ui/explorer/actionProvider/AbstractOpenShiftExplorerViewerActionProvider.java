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
package org.jboss.tools.openshift.express.internal.ui.explorer.actionProvider;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractOpenShiftAction;

/**
 * @author Xavier Coulon
 */
public abstract class AbstractOpenShiftExplorerViewerActionProvider extends CommonActionProvider {

	protected final AbstractOpenShiftAction action;
	
	protected ICommonActionExtensionSite actionExtensionSite;

	private final String group;
	
	public AbstractOpenShiftExplorerViewerActionProvider(AbstractOpenShiftAction action, String group) {
		this.action = action;
		this.group = group;
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
		if (action != null/* && action.isEnabled()*/) {
			action.validate();
			menu.appendToGroup(group, action);
		}
	}

}