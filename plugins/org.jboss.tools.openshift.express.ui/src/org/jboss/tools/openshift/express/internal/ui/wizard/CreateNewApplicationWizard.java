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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class CreateNewApplicationWizard extends Wizard implements INewWizard {

	private CreateNewApplicationWizardModel wizardModel;

	public CreateNewApplicationWizard() {
		this.wizardModel = new CreateNewApplicationWizardModel();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		this.wizardModel = new CreateNewApplicationWizardModel();
		addPage(new CredentialsWizardPage(this, wizardModel));
		addPage(new ApplicationConfigurationWizardPage(this, wizardModel));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, wizardModel));
		addPage(new GitCloningSettingsWizardPage(this, wizardModel));
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performFinish() {
		boolean successfull = true;
		if (wizardModel.getApplication() == null) {
			successfull = processApplicationCreation();

		}
		if (successfull) {
			successfull = processCartridges();
		}
		if(successfull) {
			try {
				final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
				Future<IStatus> jobResult =
						WizardUtils.runInWizard(
								new ImportJob(delegatingMonitor),
								delegatingMonitor, getContainer());
				return JobUtils.isOk(jobResult.get(10, TimeUnit.SECONDS));
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
					new Job(NLS.bind("Creating application \"{0}\"...", wizardModel.getApplicationName())) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								wizardModel.createApplication(monitor);
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
					new Job(NLS.bind("Adding selected embedded cartridges for application {0}...", wizardModel
							.getApplication().getName())) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {

								List<IEmbeddableCartridge> selectedCartridges = wizardModel
										.getSelectedEmbeddableCartridges();
								final IApplication application = wizardModel.getApplication();
								if (selectedCartridges != null && !selectedCartridges.isEmpty()) {
									application.addEmbbedCartridges(selectedCartridges);
								}
								queue.offer(true);
							} catch (OpenShiftException e) {
								queue.offer(false);
								return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind(
										"Could not embed cartridges to application {0}", wizardModel.getApplication()
												.getName()), e);
							}
							return Status.OK_STATUS;
						}
					}, getContainer());
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isTransportException(InvocationTargetException e) {
		return e.getTargetException() instanceof JGitInternalException
				&& e.getTargetException().getCause() instanceof TransportException;
	}

	private TransportException getTransportException(InvocationTargetException e) {
		if (isTransportException(e)) {
			return (TransportException) ((JGitInternalException) e.getTargetException()).getCause();
		}
		return null;
	}
	
	private void openError(final String title, final String message) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openError(getShell(), title, message);
			}
		});
	}

	private boolean askForConfirmation(final String message, final String applicationName) {
		final boolean[] confirmed = new boolean[1];
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				confirmed[0] = MessageDialog.openConfirm(
						getShell(),
						NLS.bind("Import OpenShift Application ", applicationName),
						message);
			}
		});
		return confirmed[0];
	}

	/**
	 * A workspace job that will create a new project or enable the selected
	 * project to be used with OpenShift.
	 */
	private class ImportJob extends WorkspaceJob {

		private DelegatingProgressMonitor delegatingMonitor;

		public ImportJob(DelegatingProgressMonitor delegatingMonitor) {
			super("Importing project to workspace...");
			this.delegatingMonitor = delegatingMonitor;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			try {
				delegatingMonitor.add(monitor);

				if (wizardModel.isNewProject()) {
					wizardModel.importProject(delegatingMonitor);
				} else if (!wizardModel.isGitSharedProject()) {
					if (!askForConfirmation(
							NLS.bind("OpenShift application {0} will be enabled on project {1} by " +
									"copying OpenShift configuration and enabling Git for the project.\n " +
									"This cannot be undone. Do you wish to continue ?",
									wizardModel.getApplicationName(), wizardModel.getProjectName()),
							wizardModel.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					wizardModel.configureUnsharedProject(delegatingMonitor);
				} else {
					if (!askForConfirmation(
							NLS.bind("OpenShift application {0} will be enabled on project {1} by copying OpenShift " +
									"configuration and adding the OpenShift git repo as remote.\n " +
									"This cannot be undone. Do you wish to continue ?",
									wizardModel.getApplicationName(), wizardModel.getProjectName()),
							wizardModel.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					wizardModel.configureGitSharedProject(delegatingMonitor);					
				}

				return Status.OK_STATUS;
			} catch (final WontOverwriteException e) {
				openError("Project already present", e.getMessage());
				return Status.CANCEL_STATUS;
			} catch (final ImportFailedException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not import maven project {0}.", e,
						wizardModel.getProjectName());
			} catch (IOException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not copy openshift configuration files to project {0}", e,
						wizardModel.getProjectName());
			} catch (OpenShiftException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not import project to the workspace.", e);
			} catch (URISyntaxException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"The url of the remote git repository is not valid", e);
			} catch (InvocationTargetException e) {
				if (isTransportException(e)) {
					TransportException te = getTransportException(e);
					return OpenShiftUIActivator
							.createErrorStatus(
									"Could not clone the repository. Authentication failed.\n"
											+ " Please make sure that you added your private key to the ssh preferences.",
									te);
				} else {
					return OpenShiftUIActivator.createErrorStatus(
							"An exception occurred while creating local git repository.", e);
				}
			} catch (Exception e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not import project to the workspace.", e);
			} finally {
				delegatingMonitor.done();
			}
		}
	}

}
