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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IProject;


/**
 * @author jeff.cantrill
 */
public class NewProjectWizard extends AbstractOpenShiftWizard<NewProjectWizardModel> {

	private NewProjectWizardPage newProjectWizardPage;

	public NewProjectWizard(Connection connection, List<IProject> projects) {
		super("Create OpenShift Project", new NewProjectWizardModel(connection, projects));
	}

	@Override
	public boolean performFinish() {
		AbstractDelegatingMonitorJob job = 
				new AbstractDelegatingMonitorJob(NLS.bind("Creating project {0}...", getModel().getProjectName())) {
			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					getModel().createProject();
					return Status.OK_STATUS;
				} catch (OpenShiftException e) {
					String problem = e.getStatus()==null?e.getMessage():e.getStatus().getMessage();
					return	OpenShiftUIActivator.statusFactory().errorStatus(
							NLS.bind("Could not create project \"{0}\": {1}", getModel().getProjectName(), problem), e);
				}
			}
		};
		
		try {
			WizardUtils.runInWizard(job, getContainer());
		} catch (InvocationTargetException | InterruptedException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Could not create project", e);
		}
		return job.getResult() != null && job.getResult().isOK();
	}

	@Override
	public void addPages() {
		addPage(this.newProjectWizardPage = new NewProjectWizardPage(getModel(), this));
	}
	
	public IProject getProject() {
		return newProjectWizardPage.getProject();
	}
}
