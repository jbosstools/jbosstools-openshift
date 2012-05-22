/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.portforward;

import org.eclipse.jface.wizard.Wizard;

import com.openshift.client.IApplication;


/**
 * @author Xavier Coulon
 *
 */
public class ApplicationPortForwardingWizard extends Wizard {

	private ApplicationPortForwardingWizardModel wizardModel;

	public ApplicationPortForwardingWizard(IApplication application) {
		this.wizardModel = new ApplicationPortForwardingWizardModel(application);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new ApplicationPortForwardingWizardPage(wizardModel, this));
	}

}
