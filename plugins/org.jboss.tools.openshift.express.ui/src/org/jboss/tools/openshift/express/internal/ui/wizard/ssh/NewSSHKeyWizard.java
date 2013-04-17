/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
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
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author André Dietisheim
 */
public class NewSSHKeyWizard extends Wizard {

	private Connection user;
	private NewSSHKeyWizardPage newSSHKeyWizardPage;
	
	public NewSSHKeyWizard(Connection user) {
		this.user = user;
		setWindowTitle("New SSH Key");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		IStatus status = newSSHKeyWizardPage.addConfiguredSSHKey();
		return status.isOK();
	}

	@Override
	public void addPages() {
		addPage(this.newSSHKeyWizardPage = new NewSSHKeyWizardPage(user, this));
	}
}
