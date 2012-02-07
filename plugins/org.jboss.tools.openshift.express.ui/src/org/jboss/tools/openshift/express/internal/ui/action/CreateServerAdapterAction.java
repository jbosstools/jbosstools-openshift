package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.express.client.IApplication;

public class CreateServerAdapterAction extends AbstractAction {

	public CreateServerAdapterAction() {
		super(OpenShiftExpressUIMessages.CREATE_SERVER_ADAPTER_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("edit.gif"));
	}

	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection) selection;
		if (selection != null && selection instanceof ITreeSelection
				&& treeSelection.getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) treeSelection.getFirstElement();
			ServerCore.getServers();
			/*
			 * EmbedCartridgeWizard wizard = new EmbedCartridgeWizard(user, user); WizardDialog dialog = new
			 * WizardDialog(Display.getCurrent().getActiveShell(), wizard); dialog.create(); dialog.open();
			 */

		}
	}
}
