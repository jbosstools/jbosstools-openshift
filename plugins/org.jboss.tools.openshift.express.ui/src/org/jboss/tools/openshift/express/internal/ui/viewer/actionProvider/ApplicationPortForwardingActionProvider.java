package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.ApplicationPortForwardingAction;

public class ApplicationPortForwardingActionProvider extends AbstractOpenShiftExplorerViewerActionProvider {

	public ApplicationPortForwardingActionProvider() {
		super(new ApplicationPortForwardingAction(), "group.showIn");
	}
}
