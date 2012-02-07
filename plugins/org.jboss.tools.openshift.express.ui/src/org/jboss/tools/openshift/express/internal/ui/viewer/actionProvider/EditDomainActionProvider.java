package org.jboss.tools.openshift.express.internal.ui.viewer.actionProvider;

import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.jboss.tools.openshift.express.internal.ui.action.EditDomainAction;

public class EditDomainActionProvider extends AbstractActionProvider {

	public EditDomainActionProvider() {
		super(new EditDomainAction(), "group.edition");
	}

}
