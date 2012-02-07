package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.express.client.IApplication;

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
		if (selection != null && selection instanceof ITreeSelection && ((ITreeSelection)selection).getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) ((ITreeSelection)selection).getFirstElement();
			
		}
	}


}
