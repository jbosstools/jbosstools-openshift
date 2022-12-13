/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;

/**
 * @author Red Hat Developers
 *
 */
public class CreateServiceWizard extends Wizard {
	
	private CreateServiceWizardPage page;
	
	public CreateServiceWizard(List<ServiceTemplate> serviceTemplates, String project, Odo odo) {
		this.addPage(this.page = new CreateServiceWizardPage(this, new CreateServiceModel(odo, serviceTemplates, project)));
		setNeedsProgressMonitor(true);
		setWindowTitle("Create service");
	}

	@Override
	public boolean performFinish() {
		return this.page.finish();
	}

}
