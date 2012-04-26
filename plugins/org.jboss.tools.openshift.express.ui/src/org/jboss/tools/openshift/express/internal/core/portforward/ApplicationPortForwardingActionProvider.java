package org.jboss.tools.openshift.express.internal.core.portforward;

import org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider.AbstractActionProvider;

public class ApplicationPortForwardingActionProvider extends AbstractActionProvider {

	public ApplicationPortForwardingActionProvider() {
		super(new ApplicationPortForwardingAction(), "group.showIn");
	}
}
