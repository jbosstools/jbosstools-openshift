/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.linuxtools.docker.core.IDockerConnection;

/**
 * {@link Wizard} to list and select a Docker Image.
 */
public class ListDockerImagesWizard extends Wizard {

	private final ListDockerImagesWizardModel model;
	
	private final ListDockerImagesWizardPage dockerImagesWizardPage;
	
	/**
	 * Constructor.
	 * @param dockerConnection the Docker connection
	 * @param filterName the name to use to start filtering images
	 */
	public ListDockerImagesWizard(final IDockerConnection dockerConnection, final String filterName) {
		this.model = new ListDockerImagesWizardModel(dockerConnection, filterName);
		this.dockerImagesWizardPage = new ListDockerImagesWizardPage(this, this.model);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(this.dockerImagesWizardPage);
		super.addPages();
	}
	
	@Override
	public boolean performFinish() {
		return true;
	}

	public String getSelectedImageName() {
		if(this.model.getSelectedDockerImage() == null) {
			return null;
		}
		return this.model.getSelectedDockerImage().getRepoName() + ':' + this.model.getSelectedDockerImage().getTag();
	}
	
	

}
