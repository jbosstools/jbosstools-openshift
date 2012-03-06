package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.express.client.IUser;

public class DeleteConnectionAction extends AbstractAction {

	public DeleteConnectionAction() {
		super(OpenShiftExpressUIMessages.DELETE_CONNECTION_ACTION);
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IUser) {
			final IUser user = (IUser) treeSelection.getFirstElement();
			UserModel.getDefault().removeUser(user);
		}
	}

	
}