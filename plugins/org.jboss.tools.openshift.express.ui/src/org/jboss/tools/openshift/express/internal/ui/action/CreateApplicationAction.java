package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

import com.openshift.express.client.IUser;

public class CreateApplicationAction extends AbstractAction  {

	/**
	 * Constructor
	 */
	public CreateApplicationAction() {
		super(OpenShiftExpressUIMessages.CREATE_APPLICATION_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("query-new.gif"));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() { 
		if (selection != null && selection instanceof ITreeSelection ) {
			Object sel = ((ITreeSelection)selection).getFirstElement();
			if( sel instanceof IUser) {
				IUser user = (IUser) sel;
				OpenShiftExpressApplicationWizard wizard = new OpenShiftExpressApplicationWizard(user, "New OpenShift Application");
				new WizardDialog(new Shell(), wizard).open();
			}
		}
	}


}
