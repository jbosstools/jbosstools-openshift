/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox;

import org.eclipse.jface.wizard.Wizard;

/**
 * @author Red Hat Developers
 *
 */
public class SandboxWizard extends Wizard {

	private final SandboxModel model;

	public SandboxWizard(SandboxModel model) {
		this.model = model;
		setNeedsProgressMonitor(true);
		setWindowTitle("Red Hat Developer Sandbox");
	}

	@Override
	public void addPages() {
		addPage(new SandboxWorkflowPage(this, model));
		addPage(new SandboxLoginPage(this, model));
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
