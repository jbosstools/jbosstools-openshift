package org.jboss.tools.openshift.express.internal.ui.viewer.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.EditDomainDialog;

import com.openshift.express.client.IUser;

public class EditDomainAction extends AbstractAction {

	public EditDomainAction() {
		super(OpenShiftExpressUIMessages.EDIT_DOMAIN_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("edit.gif"));
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IUser) {
			final IUser user = (IUser) treeSelection.getFirstElement();
			EditDomainDialog wizard = new EditDomainDialog(user);
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
			dialog.create();
			dialog.open();
			
		}
	}

	
}
