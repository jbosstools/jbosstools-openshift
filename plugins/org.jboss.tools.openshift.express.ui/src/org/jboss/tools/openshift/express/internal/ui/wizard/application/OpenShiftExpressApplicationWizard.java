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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.EmbedCartridgesJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizardPage;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public abstract class OpenShiftExpressApplicationWizard extends Wizard implements IImportWizard, INewWizard {

	private static final int APP_CREATE_TIMEOUT = 6 * 60 * 1000;
	private static final int APP_WAIT_TIMEOUT = 10 * 60 * 1000;
	private static final long EMBED_CARTRIDGES_TIMEOUT = 2 * 60 * 1000;
	private static final int IMPORT_TIMEOUT = 5 * 60 * 1000;

	private final boolean skipCredentialsPage;

	private final OpenShiftExpressApplicationWizardModel wizardModel;

	OpenShiftExpressApplicationWizard(final boolean useExistingApplication, final String wizardTitle) {
		this(null, null, null, useExistingApplication, wizardTitle);
	}

	OpenShiftExpressApplicationWizard(Connection user, IProject project, IApplication application,
			boolean useExistingApplication, String wizardTitle) {
		setWindowTitle(wizardTitle);
		setNeedsProgressMonitor(true);
		this.wizardModel = new OpenShiftExpressApplicationWizardModel(user, project, application,
				useExistingApplication);
		this.skipCredentialsPage = (user != null && user.isConnected());
	}

	OpenShiftExpressApplicationWizardModel getWizardModel() {
		return wizardModel;
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

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object o = selection.getFirstElement();
		if (o instanceof Connection) {
			setUser((Connection) o);
		}
	}

	protected void setUser(Connection user) {
		getWizardModel().setConnection(user);
	}

	@Override
	public void addPages() {
		if (!skipCredentialsPage) {
			addPage(new ConnectionWizardPage(this, getWizardModel()));
		}
		addPage(new ApplicationConfigurationWizardPage(this, getWizardModel()));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, getWizardModel()));
		addPage(new GitCloningSettingsWizardPage(this, getWizardModel()));
	}

	@Override
	public boolean performFinish() {
		if (!getWizardModel().isUseExistingApplication()) {

			IStatus status = createApplication();
			if (!processStatus("creating the application", status)) {
				return false;
			}

			status = waitForApplication(wizardModel.getApplication());
			if (!processStatus("waiting to become reachable", status)) {
				return false;
			}

			if (!addCartridges(
					getWizardModel().getApplication(),
					getWizardModel().getSelectedEmbeddableCartridges())) {
				return false;
			}
		}

		boolean success = importProject();

		wizardModel.fireConnectionChanged();
		
		return success;
	}

	private boolean processStatus(String operation, IStatus status) {
		if (JobUtils.isCancel(status)) {
			if (AbstractDelegatingMonitorJob.TIMEOUTED_CANCELLED == status.getCode()) {
				closeWizard();
//			} else {
//				new ErrorDialog(getShell(),
//						NLS.bind("Operation was cancelled", operation),
//						NLS.bind("we timeouted while {0}. We therefore cancelled the operation", operation),
//						status,
//						IStatus.ERROR | IStatus.WARNING | IStatus.CANCEL | IStatus.INFO)
//						.open();
			}
		}

		if (!JobUtils.isOk(status)) {
			safeRefreshUser();
			return false;
		}
		return true;
	}

	private void closeWizard() {
		IWizardContainer container = getContainer();
		if (container instanceof WizardDialog) {
			((WizardDialog) container).close();
		}
	}

	private IStatus waitForApplication(IApplication application) {
		try {
			AbstractDelegatingMonitorJob job = new WaitForApplicationJob(application, getShell());
			IStatus status = WizardUtils.runInWizard(
					job, job.getDelegatingProgressMonitor(), getContainer(), APP_WAIT_TIMEOUT);
			return status;
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Could not wait for application {0} to become reachable", application.getName()), e);
		}
	}

	private boolean importProject() {
		try {
			final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			IStatus jobResult = WizardUtils.runInWizard(
					new ImportJob(delegatingMonitor), delegatingMonitor, getContainer(), IMPORT_TIMEOUT);
			return JobUtils.isOk(jobResult);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.", OpenShiftUIActivator
					.createErrorStatus("An exception occurred while creating local git repository.", e));
			return false;
		}
	}

	private IStatus createApplication() {
		try {
			CreateApplicationJob job = new CreateApplicationJob(
					wizardModel.getApplicationName()
					, wizardModel.getApplicationCartridge()
					, wizardModel.getApplicationScale()
					, wizardModel.getApplicationGearProfile()
					, wizardModel.getConnection().getDefaultDomain());
			IStatus status = WizardUtils.runInWizard(
					job, job.getDelegatingProgressMonitor(), getContainer(), APP_CREATE_TIMEOUT);
			wizardModel.setApplication(job.getApplication());
			return status;
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Could not create application {0}", wizardModel.getApplicationName()), e);
		}
	}

	private boolean addCartridges(final IApplication application,
			final Set<IEmbeddableCartridge> selectedCartridges) {
		try {
			EmbedCartridgesJob job = new EmbedCartridgesJob(
					new ArrayList<IEmbeddableCartridge>(wizardModel.getSelectedEmbeddableCartridges()),
					true, // dont remove cartridges
					wizardModel.getApplication());
			IStatus result = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer(),
					EMBED_CARTRIDGES_TIMEOUT);
			if (result.isOK()) {
				openLogDialog(job.getAddedCartridges());
			}
			return result.isOK();
		} catch (Exception e) {
			return false;
		}
	}

	private void openLogDialog(final List<IEmbeddedCartridge> embeddableCartridges) {
		if (embeddableCartridges == null
				|| embeddableCartridges.isEmpty()) {
			return;
		}
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), embeddableCartridges).open();
			}
		});
	}

	private void safeRefreshUser() {
		try {
			wizardModel.getConnection().refresh();
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(e);
		}
	}

	/**
	 * A workspace job that will create a new project or enable the selected
	 * project to be used with OpenShift.
	 */
	class ImportJob extends WorkspaceJob {

		private DelegatingProgressMonitor delegatingMonitor;

		public ImportJob(DelegatingProgressMonitor delegatingMonitor) {
			super("Importing project to workspace...");
			setRule(ResourcesPlugin.getWorkspace().getRoot());
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
							NLS.bind(
									"OpenShift application {0} will be enabled on project {1} by copying OpenShift configuration " +
									"from server to local project and connecting local project to OpenShift Git repository.\n" +
									"The local project will be automatically committed to local Git repository and further publishing will " +
									"eventually override existing remote content.\n\n" +
									"This cannot be undone. Do you wish to continue?",
									wizardModel.getApplicationName(), wizardModel.getProjectName()),
							wizardModel.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					getWizardModel().configureUnsharedProject(delegatingMonitor);
				} else {
					if (!askForConfirmation(
							NLS.bind(
									"OpenShift application {0} will be enabled on project {1} by copying OpenShift configuration " +
									"from server to local project and connecting local project to OpenShift Git repository.\n" +
									"The local project will be automatically committed to local Git repository and further publishing will " +
									"eventually override existing remote content.\n\n" +
									"This cannot be undone. Do you wish to continue?",
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
				TransportException te = getTransportException(e);
				if (te != null) {
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

	protected TransportException getTransportException(Throwable t) {
		if (t instanceof TransportException) {
			return (TransportException) t;
		} else if (t instanceof InvocationTargetException) {
			return getTransportException(((InvocationTargetException) t).getTargetException());
		} else if (t instanceof Exception) {
			return getTransportException(((Exception) t).getCause());
		}
		return null;
	}

	@Override
	public void dispose() {
		wizardModel.dispose();
	}

}
