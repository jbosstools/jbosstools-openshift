/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.EditDomainDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.NewDomainDialog;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class CreateOrEditDomainAction extends AbstractOpenShiftAction {

	public CreateOrEditDomainAction() {
		super(OpenShiftExpressUIMessages.CREATE_OR_EDIT_DOMAIN_ACTION);
		setImageDescriptor(OpenShiftImages.EDIT);
	}

	@Override
	public void run() {
		Connection connection = UIUtils.getFirstElement(getSelection(), Connection.class);
		if (connection == null) {
			return;
		}

		// do not show the dialog if the user was not connected or did not provide valid credentials.
		if (!connection.connect()) {
			return;
		}
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), createDomainWizard(connection));
		dialog.create();
		dialog.open();
	}

	private IWizard createDomainWizard(final Connection user) {
		IWizard domainWizard;
		try {
			if (user.getDefaultDomain() == null || user.getDefaultDomain().getId() == null) {
				domainWizard = new NewDomainDialog(user);
			} else {
				domainWizard = new EditDomainDialog(user);
			}
		} catch (OpenShiftException e) {
			Logger.warn("Failed to retrieve User domain, prompting for creation", e);
			// let's use the domain creation wizard, then.
			domainWizard = new NewDomainDialog(user);
		}
		return domainWizard;
	}	
}
