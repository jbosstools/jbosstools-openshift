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
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.jface.wizard.Wizard;

import com.openshift.client.IApplication;

/**
 * @author Andr?? Dietisheim
 */
public class TailFilesWizard extends Wizard {

	private final TailFilesWizardPageModel model;

	public TailFilesWizard(final IApplication app) {
		setNeedsProgressMonitor(true);
		setWindowTitle("Tail Files");
		this.model = new TailFilesWizardPageModel(app);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new TailFilesWizardPage(model, this));
	}
	
	public String getFilePattern() {
		return model.getFilePattern();
	}

	/**
	 * @return true if the 'tail' command be executed on all gears (if the
	 *         application is scalable), false otherwise.
	 */
	public boolean isAllGears() {
		return model.getAllGears();
	}

}
