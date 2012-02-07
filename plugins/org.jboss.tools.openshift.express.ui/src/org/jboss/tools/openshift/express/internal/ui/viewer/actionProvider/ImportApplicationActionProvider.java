package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.jboss.tools.openshift.express.internal.ui.action.ImportApplicationAction;


public class ImportApplicationActionProvider extends AbstractActionProvider {

	public ImportApplicationActionProvider() {
		super(new ImportApplicationAction(), "group.server");
	}

}
