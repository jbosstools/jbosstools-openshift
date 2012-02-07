package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.TailServerLogAction;


public class TailServerLogActionProvider extends AbstractActionProvider {

	public TailServerLogActionProvider() {
		super(new TailServerLogAction(), "group.showIn");
	}
}
