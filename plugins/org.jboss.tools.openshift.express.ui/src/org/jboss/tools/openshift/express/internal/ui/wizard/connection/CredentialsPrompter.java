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
package org.jboss.tools.openshift.express.internal.ui.wizard.connection;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ICredentialsPrompter;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class CredentialsPrompter implements ICredentialsPrompter {

	@Override
	public void promptAndAuthenticate(final Connection connection) {
		Display.getDefault().syncExec(
				new Runnable() {
					public void run() {
						Shell shell = UIUtils.getShell();
						if (shell == null) {
							Logger.error("Could not open Credentials wizard: no shell available");
							return;
						}
						
						final ConnectionWizard connectToOpenShiftWizard =
								new ConnectionWizard(connection, false);
						WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
					}
				});
	}

}
