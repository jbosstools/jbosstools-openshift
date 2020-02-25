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

import org.eclipse.jface.wizard.Wizard;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentWizard extends Wizard {
	
	public CreateComponentWizard(CreateComponentModel model) {
		this.addPage(new CreateComponentWizardPage(this, model));
		setNeedsProgressMonitor(true);
		setWindowTitle("Create component");
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
