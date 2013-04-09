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

import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.details.ApplicationDetailsDialog;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class ShowDetailsAction extends AbstractOpenShiftAction {

	public ShowDetailsAction() {
		super(OpenShiftExpressUIMessages.SHOW_DETAILS_ACTION, true);
	}

	@Override
	public void run() {
		try {
			final IApplication application = UIUtils.getFirstElement(getSelection(), IApplication.class);
			if (application == null) {
				return;
			}
			new ApplicationDetailsDialog(application, Display.getDefault().getActiveShell()).open();
		} catch (Exception e) {
			OpenShiftUIActivator.createErrorStatus("Failed to display application details", e);
		}

	}
	
}
