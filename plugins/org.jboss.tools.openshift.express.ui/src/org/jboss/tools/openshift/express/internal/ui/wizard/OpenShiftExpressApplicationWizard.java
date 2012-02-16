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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IImportWizard;
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
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public abstract class OpenShiftExpressApplicationWizard extends Wizard implements IImportWizard, INewWizard {

	private IUser initialUser;

	private OpenShiftExpressApplicationWizardModel wizardModel;

	/**
	 * @see #setUser called by CredentialsWizardPageModel#getValidityStatus
	 */
	public OpenShiftExpressApplicationWizard(String wizardTitle) {
		this(null, null, null, wizardTitle);
	}

	public OpenShiftExpressApplicationWizard(IUser user, IProject project, IApplication application, String wizardTitle) {
		setWizardModel(new OpenShiftExpressApplicationWizardModel(user, project, application));
		setWindowTitle(wizardTitle);
		setNeedsProgressMonitor(true);
	}

	void setWizardModel(OpenShiftExpressApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	OpenShiftExpressApplicationWizardModel getWizardModel() {
		return wizardModel;
	}

	protected boolean isTransportException(InvocationTargetException e) {
		return e.getTargetException() instanceof JGitInternalException
				&& e.getTargetException().getCause() instanceof TransportException;
	}

	protected TransportException getTransportException(InvocationTargetException e) {
		if (isTransportException(e)) {
			return (TransportException) ((JGitInternalException) e.getTargetException()).getCause();
		}
		return null;
	}

	protected void openError(final String title, final String message) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openError(getShell(), title, message);
			}
		});
	}

	protected boolean askForConfirmation(final String message, final String applicationName) {
		final boolean[] confirmed = new boolean[1];
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				confirmed[0] = MessageDialog.openConfirm(getShell(),
						NLS.bind("Import OpenShift Application ", applicationName), message);
			}
		});
		return confirmed[0];
	}

	public void setSelectedApplication(IApplication application) {
		getWizardModel().setApplication(application);
	}

	public void setSelectedProject(IProject project) {
		getWizardModel().setProject(project);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object o = selection.getFirstElement();
		if (o instanceof IUser) {
			setUser((IUser) o);
		}
	}

	protected void setUser(IUser user) {
		getWizardModel().setUser(user);
	}

	@Override
	public void addPages() {
		addPage(new CredentialsWizardPage(this, getWizardModel()));
		addPage(new ApplicationConfigurationWizardPage(this, getWizardModel()));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, getWizardModel()));
		addPage(new GitCloningSettingsWizardPage(this, getWizardModel()));
	}

	@Override
	public IWizardPage getStartingPage() {
		IWizardPage[] pages = getPages();
		return initialUser == null ? pages[0] : pages[1];
	}

	public void setInitialUser(IUser user) {
		this.initialUser = user;
	}

	@Override
	public boolean performFinish() {
		boolean success = getWizardModel().isUseExistingApplication();
		if (!success) {
			if(createApplication()) {
				success = addRemoveCartridges(
						getWizardModel().getApplication(), getWizardModel().getSelectedEmbeddableCartridges());
			}
		}
		if (success) {
			success = importProject();
		}

		wizardModel.addUserToModel();
		return success;
	}

	private boolean importProject() {
		try {
			final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			IStatus jobResult = WizardUtils.runInWizard(new ImportJob(delegatingMonitor), delegatingMonitor,
					getContainer());
			return JobUtils.isOk(jobResult);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.", OpenShiftUIActivator
					.createErrorStatus("An exception occurred while creating local git repository.", e));
			return false;
		}
	}

	private boolean createApplication() {
		try {
			final String applicationName = wizardModel.getApplicationName();
			IStatus status = WizardUtils.runInWizard(
					new Job(NLS.bind("Creating application \"{0}\"...", applicationName)) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								getWizardModel().createApplication(monitor);
								return Status.OK_STATUS;
							} catch (Exception e) {
								// TODO: refresh user
								return OpenShiftUIActivator.createErrorStatus("Could not create application \"{0}\"",
										e, applicationName);
							}
						}

					}, getContainer());
			return status.isOK();
		} catch (Exception e) {
			return false;
		}
	}

	private boolean addRemoveCartridges(final IApplication application,
			final Set<IEmbeddableCartridge> selectedCartridges) {
		try {
			IStatus status = WizardUtils.runInWizard(
					new Job(NLS.bind("Adding selected embedded cartridges for application {0}...", getWizardModel()
							.getApplication().getName())) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								if (selectedCartridges != null && !selectedCartridges.isEmpty()) {
									List<IEmbeddableCartridge> embeddableCartridges = new ArrayList<IEmbeddableCartridge>();
									embeddableCartridges.addAll(selectedCartridges);
									application.addEmbbedCartridges(embeddableCartridges);
								}
							} catch (OpenShiftException e) {
								return OpenShiftUIActivator.createErrorStatus(NLS.bind(
										"Could not embed cartridges to application {0}", getWizardModel()
												.getApplication().getName()), e);
							}
							return Status.OK_STATUS;
						}
					}, getContainer());
			return status.isOK();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * A workspace job that will create a new project or enable the selected project to be used with OpenShift.
	 */
	class ImportJob extends WorkspaceJob {

		private DelegatingProgressMonitor delegatingMonitor;

		public ImportJob(DelegatingProgressMonitor delegatingMonitor) {
			super("Importing project to workspace...");
			this.delegatingMonitor = delegatingMonitor;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			try {
				delegatingMonitor.add(monitor);
				OpenShiftExpressApplicationWizardModel wizardModel = getWizardModel();
				if (wizardModel.isNewProject()) {
					wizardModel.importProject(delegatingMonitor);
				} else if (!wizardModel.isGitSharedProject()) {
					if (!askForConfirmation(
							NLS.bind("OpenShift application {0} will be enabled on project {1} by "
									+ "copying OpenShift configuration and enabling Git for the project.\n "
									+ "This cannot be undone. Do you wish to continue ?",
									wizardModel.getApplicationName(), wizardModel.getProjectName()),
							wizardModel.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					getWizardModel().configureUnsharedProject(delegatingMonitor);
				} else {
					if (!askForConfirmation(
							NLS.bind("OpenShift application {0} will be enabled on project {1} by copying OpenShift "
									+ "configuration and adding the OpenShift git repo as remote.\n "
									+ "This cannot be undone. Do you wish to continue ?",
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
				return OpenShiftUIActivator.createErrorStatus("Could not import maven project {0}.", e,
						getWizardModel().getProjectName());
			} catch (IOException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not copy openshift configuration files to project {0}", e, getWizardModel()
								.getProjectName());
			} catch (OpenShiftException e) {
				return OpenShiftUIActivator.createErrorStatus("Could not import project to the workspace.", e);
			} catch (URISyntaxException e) {
				return OpenShiftUIActivator.createErrorStatus("The url of the remote git repository is not valid", e);
			} catch (InvocationTargetException e) {
				if (isTransportException(e)) {
					TransportException te = getTransportException(e);
					return OpenShiftUIActivator.createErrorStatus(
							"Could not clone the repository. Authentication failed.\n"
									+ " Please make sure that you added your private key to the ssh preferences.", te);
				} else {
					return OpenShiftUIActivator.createErrorStatus(
							"An exception occurred while creating local git repository.", e);
				}
			} catch (Exception e) {
				return OpenShiftUIActivator.createErrorStatus("Could not import project to the workspace.", e);
			} finally {
				delegatingMonitor.done();
			}
		}
	}

	@Override
	public void dispose() {
		wizardModel.dispose();
	}

}
