package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

public class ShowPropertiesAction extends AbstractAction {

	public ShowPropertiesAction() {
		super(OpenShiftExpressUIMessages.SHOW_PROPERTIES_VIEW_ACTION);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.ui.views.PropertySheet");
		} catch (PartInitException e) {
			Logger.error("Failed to show properties view", e);
		} 
	}

}
