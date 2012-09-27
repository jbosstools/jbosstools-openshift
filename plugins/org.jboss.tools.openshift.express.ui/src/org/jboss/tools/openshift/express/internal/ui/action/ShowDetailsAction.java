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

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.details.ApplicationDetailsDialog;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class ShowDetailsAction extends AbstractAction {

	public ShowDetailsAction() {
		super(OpenShiftExpressUIMessages.SHOW_DETAILS_ACTION, true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		try {
			final ITreeSelection treeSelection = (ITreeSelection) selection;
			if (selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
				final IApplication application = (IApplication) treeSelection.getFirstElement();
				new ApplicationDetailsDialog(application, Display.getDefault().getActiveShell()).open();
			}
		} catch (Exception e) {
			OpenShiftUIActivator.createErrorStatus("Failed to display application details", e);
		}

	}
	
}
