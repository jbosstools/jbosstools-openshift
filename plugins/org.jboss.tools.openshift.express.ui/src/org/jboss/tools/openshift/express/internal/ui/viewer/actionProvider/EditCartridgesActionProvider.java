package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.EditCartridgesAction;


public class EditCartridgesActionProvider extends AbstractActionProvider {

	public EditCartridgesActionProvider() {
		super(new EditCartridgesAction(), "group.edition");
	}

}
