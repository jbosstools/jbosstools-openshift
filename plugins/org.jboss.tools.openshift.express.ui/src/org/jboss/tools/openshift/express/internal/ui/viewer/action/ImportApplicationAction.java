package org.jboss.tools.openshift.express.internal.ui.viewer.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.EmbedCartridgeWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.ImportExistingApplicationWizard;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;

public class ImportApplicationAction extends AbstractAction {

	public ImportApplicationAction() {
		super(OpenShiftExpressUIMessages.IMPORT_APPLICATION_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("go-into.gif"));
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) treeSelection.getFirstElement();
			final IUser user = OpenShiftUIActivator.getDefault().getUser();
			ImportExistingApplicationWizard wizard = new ImportExistingApplicationWizard();
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
			dialog.create();
			dialog.open();
			
		}
	}

	
}
