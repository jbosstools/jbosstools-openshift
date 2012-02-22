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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.NewOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

import com.openshift.express.client.IUser;

/**
 * @author Xavier Coulon
 */
public class CreateApplicationAction extends AbstractAction  {

	/**
	 * Constructor
	 */
	public CreateApplicationAction() {
		super(OpenShiftExpressUIMessages.CREATE_APPLICATION_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("query-new.gif"));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() { 
		if (selection != null && selection instanceof ITreeSelection ) {
			Object sel = ((ITreeSelection)selection).getFirstElement();
			if( sel instanceof IUser) {
				IUser user = (IUser) sel;
				OpenShiftExpressApplicationWizard wizard = new NewOpenShiftExpressApplicationWizard(user);
				new WizardDialog(new Shell(), wizard).open();
			}
		}
	}


}
