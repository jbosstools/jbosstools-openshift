package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.CreateApplicationAction;



public class CreateApplicationActionProvider extends AbstractActionProvider {

	public CreateApplicationActionProvider() {
		super(new CreateApplicationAction(), "group.edition");
	}

}
