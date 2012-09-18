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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

/**
 * @author André Dietisheim
 */
public class AddSSHKeyWizard extends Wizard {

	private UserDelegate user;
	private AddSSHKeyWizardPage addSSHKeyWizardPage;
	
	public AddSSHKeyWizard(UserDelegate user) {
		this.user = user;
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		IStatus status = addSSHKeyWizardPage.addConfiguredSSHKey();
		return status.isOK();
	}

	@Override
	public void addPages() {
		addPage(this.addSSHKeyWizardPage = new AddSSHKeyWizardPage(user, this));
	}
}
