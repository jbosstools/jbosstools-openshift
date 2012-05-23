package org.jboss.tools.openshift.express.internal.ui.serverviewer.actionDelegate;

import org.jboss.tools.openshift.express.internal.ui.console.TailServerLogAction;

public class TailServerLogActionProvider extends AbstractServerViewerActionProvider {

	public TailServerLogActionProvider() {
		super(new TailServerLogAction());
	}
	
}
