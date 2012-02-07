package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.DeleteApplicationAction;


public class DeleteApplicationActionProvider extends AbstractActionProvider {

	public DeleteApplicationActionProvider() {
		super(new DeleteApplicationAction(), "group.edition");
	}

}
