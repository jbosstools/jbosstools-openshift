package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.ui.views.server.extensions.CommonActionProviderUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;

public class TailServerLogActionProvider extends CommonActionProvider {

	private TailServerLogAction action;

	private ICommonActionExtensionSite actionExtensionSite;

	public void init(ICommonActionExtensionSite actionExtensionSite) {
		super.init(actionExtensionSite);
		this.actionExtensionSite = actionExtensionSite;
		ICommonViewerSite site = actionExtensionSite.getViewSite();
		if (site instanceof ICommonViewerWorkbenchSite) {
			action = new TailServerLogAction();
			action.setSelection(actionExtensionSite.getStructuredViewer().getSelection());
			actionExtensionSite.getStructuredViewer().addSelectionChangedListener(action);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		if (action != null && action.isEnabled()) {
			Object sel = getSelection();
			if( sel instanceof IServer ) {
				IServer server = (IServer)sel;
				if (ExpressServerUtils.isOpenShiftRuntime(server) || ExpressServerUtils.isInOpenshiftBehaviourMode(server)) {
					//menu.insertBefore(ServerActionProvider.CONTROL_SERVER_SECTION_END_SEPARATOR, action);
					CommonActionProviderUtils.addToShowInQuickSubMenu(action, menu, actionExtensionSite);
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
