package org.jboss.tools.openshift.express.internal.ui.serverviewer.actionDelegate;

import org.jboss.tools.openshift.express.internal.ui.action.ApplicationPortForwardingAction;

public class ApplicationPortForwardingActionProvider extends AbstractServerViewerActionProvider {

	public ApplicationPortForwardingActionProvider() {
		super(new ApplicationPortForwardingAction());
	}
}
