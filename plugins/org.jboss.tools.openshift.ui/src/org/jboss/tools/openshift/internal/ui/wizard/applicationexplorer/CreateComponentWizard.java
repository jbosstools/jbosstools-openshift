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

import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.core.odo.ComponentType;
import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class CreateComponentWizard extends Wizard {
	
	private CreateComponentWizardPage page;
	
	public CreateComponentWizard(List<ComponentType> componentTypes, String project, String application, Odo odo) {
		this.addPage(this.page = new CreateComponentWizardPage(this, new CreateComponentModel(odo, componentTypes, project, application)));
		setNeedsProgressMonitor(true);
		setWindowTitle("Create component");
	}

	@Override
	public boolean performFinish() {
		return this.page.finish();
	}

}
