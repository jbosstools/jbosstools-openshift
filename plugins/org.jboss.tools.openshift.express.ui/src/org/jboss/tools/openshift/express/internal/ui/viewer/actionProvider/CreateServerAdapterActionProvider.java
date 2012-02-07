package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.CreateServerAdapterAction;

public class CreateServerAdapterActionProvider extends AbstractActionProvider {

	public CreateServerAdapterActionProvider() {
		super(new CreateServerAdapterAction(), "group.server");
	}

}
