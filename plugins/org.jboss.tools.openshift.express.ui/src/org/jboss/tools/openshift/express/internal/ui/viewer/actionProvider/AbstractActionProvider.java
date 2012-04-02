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
package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractAction;

/**
 * @author Xavier Coulon
 */
public abstract class AbstractActionProvider extends CommonActionProvider {

	protected final AbstractAction action;
	
	protected ICommonActionExtensionSite actionExtensionSite;

	private final String group;
	
	public AbstractActionProvider(AbstractAction action, String group) {
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
		action.validate();
		if (action != null/* && action.isEnabled()*/) {
			menu.appendToGroup(group, action);
		}
	}

}