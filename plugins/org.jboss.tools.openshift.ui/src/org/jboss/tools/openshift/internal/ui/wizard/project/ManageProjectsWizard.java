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
package org.jboss.tools.openshift.internal.ui.wizard.project;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.model.IProject;


/**
 * @author jeff.cantrill
 */
public class ManageProjectsWizard extends AbstractOpenShiftWizard<ManageProjectsWizardPageModel> {

	private String description;
	private ManageProjectsWizardPage manageProjectsWizardPage;

	public ManageProjectsWizard(Connection connection) {
		this(null, connection);
	}

	public ManageProjectsWizard(IProject project, Connection connection) {
		super("OpenShift Projects", new ManageProjectsWizardPageModel(project, connection));
		this.description = NLS.bind("Manage projects for connection {0}", connection.toString(), connection);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	@Override
	public void addPages() {
		addPage(this.manageProjectsWizardPage = new ManageProjectsWizardPage(getWindowTitle(), description, getModel(), this));
	}
	
	public IProject getSelectedProject() {
		if (manageProjectsWizardPage == null) {
			return null;
		}
		return manageProjectsWizardPage.getSelectedProject();
	}

}
