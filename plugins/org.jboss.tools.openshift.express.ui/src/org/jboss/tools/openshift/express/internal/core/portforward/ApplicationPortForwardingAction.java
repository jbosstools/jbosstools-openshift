package org.jboss.tools.openshift.express.internal.core.portforward;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractAction;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;

public class ApplicationPortForwardingAction extends AbstractAction {

	public ApplicationPortForwardingAction() {
		super("Port forwarding...", DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT));
	}
	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() { 
		if (selection != null && selection instanceof ITreeSelection ) {
			Object sel = ((ITreeSelection)selection).getFirstElement();
			if( sel instanceof IApplication) {
				IApplication application = (IApplication)sel;
				try {
					//TitleD
					ApplicationPortForwardingWizard wizard = new ApplicationPortForwardingWizard(application);
					WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
					dialog.setMinimumPageSize(700, 300);
					dialog.create();
					dialog.open();
				} catch (Exception e) {
					Logger.error("Failed to perform 'port-forwarding' for application '" + application.getName() + "'", e);
				}
			}
		}
	}


}
