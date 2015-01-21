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

import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

/**
 * @author André Dietisheim
 */
public class ManageSSHKeysWizard extends AbstractOpenShiftWizard<ExpressConnection> {

	public ManageSSHKeysWizard(ExpressConnection connection) {
		super("Manage SSH Keys", connection);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new ManageSSHKeysWizardPage(getModel(), this));
	}
}
