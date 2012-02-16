package org.jboss.tools.openshift.express.internal.ui.viewer.actionDelegate;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.jboss.tools.openshift.express.internal.ui.console.ExpressConsoleView;

public class RefreshViewerActionDelegate implements IViewActionDelegate {

	private ExpressConsoleView view;

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
		if (view instanceof ExpressConsoleView) {
			this.view = (ExpressConsoleView) view;
		}
	}

}
