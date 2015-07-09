/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.portforwading;

import org.eclipse.jface.wizard.Wizard;

/**
 * @author jeff.cantrill
 */
public class PortForwardingWizard extends Wizard  {

	private PortForwardingWizardModel model;
	public PortForwardingWizard(PortForwardingWizardModel model) {
		setWindowTitle("Application Port Forwarding");
		this.model = model;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new PortForwardingWizardPage(model, this));
	}

}
