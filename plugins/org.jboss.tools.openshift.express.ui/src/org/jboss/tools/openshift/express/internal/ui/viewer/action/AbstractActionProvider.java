package org.jboss.tools.openshift.express.internal.ui.viewer.action;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public abstract class AbstractActionProvider extends CommonActionProvider {

	private final AbstractAction action;
	
	private final String group;
	
	public AbstractActionProvider(AbstractAction action, String group) {
		this.action = action;
		this.group = group;
	}

	public void init(ICommonActionExtensionSite actionExtensionSite) {
		super.init(actionExtensionSite);
		ICommonViewerSite site = actionExtensionSite.getViewSite();
		if (site instanceof ICommonViewerWorkbenchSite) {
			action.setSelection(actionExtensionSite.getStructuredViewer().getSelection());
			actionExtensionSite.getStructuredViewer().addSelectionChangedListener(action);
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (action != null && action.isEnabled()) {
			menu.appendToGroup(group, action);
		}
	}

}