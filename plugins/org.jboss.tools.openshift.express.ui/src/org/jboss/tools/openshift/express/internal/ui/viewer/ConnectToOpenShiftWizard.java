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
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.wizard.ConnectToOpenShiftWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPage;
import org.w3c.dom.UserDataHandler;

/**
 * @author Xavier Coulon
 */
public class ConnectToOpenShiftWizard extends Wizard {

	private final CredentialsWizardPage page = new CredentialsWizardPage(this, new ConnectToOpenShiftWizardModel());
	
	public ConnectToOpenShiftWizard() {
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public boolean performFinish() {
		if(page.performAuthentication()) {
			return true;
		}
		return false;
	}

	@Override
	public void addPages() {
		addPage(page);
	}
	
	public UserDelegate getUser() {
		return page.getUser();
	}
}
