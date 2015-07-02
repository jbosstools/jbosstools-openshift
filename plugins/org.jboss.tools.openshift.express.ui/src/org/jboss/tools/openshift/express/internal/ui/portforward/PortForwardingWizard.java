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
package org.jboss.tools.openshift.express.internal.ui.portforward;

import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IApplication;


/**
 * @author Xavier Coulon
 *
 */
public class PortForwardingWizard extends AbstractOpenShiftWizard<PortForwardingWizardModel> {

	public PortForwardingWizard(IApplication application) {
		super("Application port forwarding", new PortForwardingWizardModel(application));
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new PortForwardingWizardPage(getModel(), this));
	}

}
