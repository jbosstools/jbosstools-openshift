package org.jboss.tools.openshift.express.internal.ui.viewer.action;

import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogAction;

public class TailServerLogActionProvider extends AbstractActionProvider {

	public TailServerLogActionProvider() {
		super(new TailServerLogAction(), "group.showIn");
	}
}
