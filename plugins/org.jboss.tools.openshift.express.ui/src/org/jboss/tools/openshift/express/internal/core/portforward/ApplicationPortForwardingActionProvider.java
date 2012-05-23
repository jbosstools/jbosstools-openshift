package org.jboss.tools.openshift.express.internal.core.portforward;

import org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider.AbstractOpenShiftExplorerViewerActionProvider;

public class ApplicationPortForwardingActionProvider extends AbstractOpenShiftExplorerViewerActionProvider {

	public ApplicationPortForwardingActionProvider() {
		super(new ApplicationPortForwardingAction(), "group.showIn");
	}
}
