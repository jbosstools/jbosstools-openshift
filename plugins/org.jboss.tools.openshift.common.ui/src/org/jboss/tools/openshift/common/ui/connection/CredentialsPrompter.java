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
package org.jboss.tools.openshift.common.ui.connection;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class CredentialsPrompter implements ICredentialsPrompter {

	public CredentialsPrompter() {
		// Do nothing
	}
	
	@Override
	public boolean promptAndAuthenticate(final IConnection connection, final Object context) {
		Display.getDefault().syncExec(
				new Runnable() {
					@Override
					public void run() {
						Shell shell = UIUtils.getShell();
						if (shell == null) {
							OpenShiftCommonUIActivator.log("Could not open Credentials wizard: no shell available", null);
							return;
						}
						
						final ConnectionWizard connectToOpenShiftWizard =
								new ConnectionWizard(connection, context);
						WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
					}
				});
		return true;
	}

}
