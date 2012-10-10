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

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author André Dietisheim
 */
public class ManageSSHKeysWizard extends Wizard {

	private Connection user;

	public ManageSSHKeysWizard(Connection user) {
		this.user = user;
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new ManageSSHKeysWizardPage(user, this));
	}
}
