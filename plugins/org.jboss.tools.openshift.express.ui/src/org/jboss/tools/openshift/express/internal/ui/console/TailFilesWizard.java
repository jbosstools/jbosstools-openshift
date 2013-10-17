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

import java.util.Collection;

import org.eclipse.jface.wizard.Wizard;

import com.openshift.client.IApplication;
import com.openshift.client.IGearGroup;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class TailFilesWizard extends Wizard {

	private final TailFilesWizardPageModel model;

	public TailFilesWizard(final IApplication app) {
		setNeedsProgressMonitor(true);
		setWindowTitle("Tail Files");
		this.model = new TailFilesWizardPageModel(app);
	}

	@Override
	public boolean canFinish() {
		final Collection<IGearGroup> selectedGearGroups = getSelectedGearGroups();
		return selectedGearGroups != null && !selectedGearGroups.isEmpty();
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
	public Collection<IGearGroup> getSelectedGearGroups() {
		return model.getSelectedGearGroups();
	}

}
