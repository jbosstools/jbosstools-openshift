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

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerUtils;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.NewOpenShiftApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizard;

/**
 * @author Xavier Coulon
 */
public class CreateApplicationAction extends AbstractOpenShiftAction {

	/**
	 * Constructor
	 */
	public CreateApplicationAction() {
		super(OpenShiftExpressUIMessages.CREATE_APPLICATION_ACTION);
		setImageDescriptor(OpenShiftImages.QUERY_NEW);
	}

	@Override
	public void run() {
		Connection connection = OpenShiftExplorerUtils.getConnectionFor(getSelection());
		if (connection == null) {
			return;
		}

		if (connection.connect()) {
			try {
				final OpenShiftApplicationWizard wizard = new NewOpenShiftApplicationWizard(connection);
				new WizardDialog(new Shell(), wizard).open();
			} catch(NullPointerException e) {
				// swallow NPE that's caused by cancelling ssh keys / domain wizard 
				// https://issues.jboss.org/browse/JBIDE-14575
			}
			
		}
	}
}
