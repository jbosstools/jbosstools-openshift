package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.OpenInWebBrowserAction;


public class OpenInWebBrowserActionProvider extends AbstractActionProvider {

	public OpenInWebBrowserActionProvider() {
		super(new OpenInWebBrowserAction(), "group.showIn");
	}

}
