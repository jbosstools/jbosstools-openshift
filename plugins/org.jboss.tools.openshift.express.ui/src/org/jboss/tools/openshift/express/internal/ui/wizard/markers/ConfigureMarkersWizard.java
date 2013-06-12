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
package org.jboss.tools.openshift.express.internal.ui.wizard.markers;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author André Dietisheim
 */
public class ConfigureMarkersWizard extends Wizard {

	private IProject project;

	public ConfigureMarkersWizard(IProject project) {
		this.project = project;
		setNeedsProgressMonitor(true);
		setWindowTitle("Configure OpenShift Markers");
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new ConfigureMarkersWizardPage(project, this));
	}
}
