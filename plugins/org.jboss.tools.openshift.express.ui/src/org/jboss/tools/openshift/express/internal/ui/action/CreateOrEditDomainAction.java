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

import java.net.SocketTimeoutException;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.EditDomainDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.NewDomainDialog;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class CreateOrEditDomainAction extends AbstractAction {

	public CreateOrEditDomainAction() {
		super(OpenShiftExpressUIMessages.CREATE_OR_EDIT_DOMAIN_ACTION);
		setImageDescriptor(OpenShiftImages.EDIT);
	}

	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection) selection;
		if (selection instanceof ITreeSelection
				&& treeSelection.getFirstElement() instanceof UserDelegate) {
			final UserDelegate user = (UserDelegate) treeSelection.getFirstElement();
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), createDomainWizard(user));
			dialog.create();
			dialog.open();
		}
	}

	private IWizard createDomainWizard(final UserDelegate user) {
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
		} catch (SocketTimeoutException e) {
			Logger.warn("Failed to retrieve User domain, prompting for creation", e);
			// let's use the domain creation wizard, then.
			domainWizard = new NewDomainDialog(user);
		}
		return domainWizard;
	}

}
