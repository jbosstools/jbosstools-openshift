package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.EditDomainDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.NewDomainDialog;

import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class CreateOrEditDomainAction extends AbstractAction {

	public CreateOrEditDomainAction() {
		super(OpenShiftExpressUIMessages.EDIT_DOMAIN_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("edit.gif"));
	}

	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection) selection;
		if (selection != null && selection instanceof ITreeSelection
				&& treeSelection.getFirstElement() instanceof IUser) {
			IWizard domainDialog = null;
			final IUser user = (IUser) treeSelection.getFirstElement();
			try {
				if (user.getDomain() == null || user.getDomain().getNamespace() == null) {
					domainDialog = new NewDomainDialog(user);
				} else {
					domainDialog = new EditDomainDialog(user);
				}
			} catch (OpenShiftException e) {
				Logger.warn("Failed to retrieve User domain, prompting for creation", e);
				// let's use the domain creation wizard, then.
				domainDialog = new NewDomainDialog(user);
			}
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), domainDialog);
			dialog.create();
			dialog.open();
		}
	}

}
