/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.core.odo.Service;

/**
 * @author Red Hat Developers
 *
 */
public class LinkServiceWizardPage extends LinkWizardPage<LinkModel<Service>> {

	protected LinkServiceWizardPage(IWizard wizard, LinkModel<Service> model) {
		super(wizard, model, "Link service", "Select a target service to bind the component to.");
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		createTargetControls(parent, dbc, "Target service:", new ServiceColumLabelProvider());
	}
}
