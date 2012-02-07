package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.ShowPropertiesAction;


public class ShowPropertiesActionProvider extends AbstractActionProvider {

	public ShowPropertiesActionProvider() {
		super(new ShowPropertiesAction(), "group.properties");
	}

}
