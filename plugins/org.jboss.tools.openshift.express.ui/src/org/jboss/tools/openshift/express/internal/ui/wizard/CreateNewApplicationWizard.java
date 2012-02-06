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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andr� Dietisheim
 * @author Xavier Coulon
 */
public class CreateNewApplicationWizard extends AbstractOpenShiftApplicationWizard<CreateNewApplicationWizardModel>
		implements INewWizard {

	public CreateNewApplicationWizard() {
		setWizardModel(new CreateNewApplicationWizardModel());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		final IUser user = OpenShiftUIActivator.getDefault().getUser();
		try {
			if (user == null 
					|| !user.isValid()) {
				addPage(new CredentialsWizardPage(this));
//			} else {
//				getWizardModel().setUser(user);
			}
		} catch (OpenShiftException e) {
			// if the user's validity can't be checked, we may want to
			// re-connect..
			addPage(new CredentialsWizardPage(this));
		}
		addPage(new ApplicationConfigurationWizardPage(this, getWizardModel()));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, getWizardModel()));
		addPage(new GitCloningSettingsWizardPage(this, getWizardModel()));
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean performFinish() {
		boolean successfull = true;
		if (getWizardModel().getApplication() == null) {
			successfull = processApplicationCreation();

		}
		if (successfull) {
			successfull = processCartridges();
		}
		if (successfull) {
			try {
				final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
				IStatus jobResult = WizardUtils.runInWizard(
						new ImportJob(delegatingMonitor), delegatingMonitor, getContainer());
				return JobUtils.isOk(jobResult);
			} catch (Exception e) {
				ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.",
						new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"An exception occurred while creating local git repository.", e));
				return false;
			}
		}
		return successfull;
	}

	private boolean processApplicationCreation() {
		final ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
		try {
			WizardUtils.runInWizard(
					new Job(NLS.bind("Creating application \"{0}\"...", getWizardModel().getApplicationName())) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								getWizardModel().createApplication(monitor);
								queue.offer(true);
								return Status.OK_STATUS;
							} catch (Exception e) {
								queue.offer(false);
								return OpenShiftUIActivator.createErrorStatus("Could not create application", e);
							}
						}

					}, getContainer());
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean processCartridges() {
		final ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);
		try {
			WizardUtils.runInWizard(
					new Job(NLS.bind("Adding selected embedded cartridges for application {0}...", getWizardModel()
							.getApplication().getName())) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {

								List<IEmbeddableCartridge> selectedCartridges =
										getWizardModel().getSelectedEmbeddableCartridges();
								final IApplication application = getWizardModel().getApplication();
								if (selectedCartridges != null 
										&& !selectedCartridges.isEmpty()) {
									application.addEmbbedCartridges(selectedCartridges);
								}
								queue.offer(true);
							} catch (OpenShiftException e) {
								queue.offer(false);
								return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind(
										"Could not embed cartridges to application {0}", getWizardModel()
												.getApplication().getName()), e);
							}
							return Status.OK_STATUS;
						}
					}, getContainer());
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			return false;
		}
	}
}
