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
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizardPage;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public abstract class OpenShiftApplicationWizard extends Wizard implements IImportWizard, INewWizard {

	private static final int APP_CREATE_TIMEOUT = 10 * 60 * 1000;
	private static final int APP_WAIT_TIMEOUT = 10 * 60 * 1000;
	private static final int IMPORT_TIMEOUT = 20 * 60 * 1000;

	private final boolean skipCredentialsPage;
	private final OpenShiftApplicationWizardModel model;

	OpenShiftApplicationWizard(final boolean useExistingApplication, final String wizardTitle) {
		this(null, null, null, useExistingApplication, wizardTitle);
	}

	OpenShiftApplicationWizard(Connection user, IProject project, IApplication application,
			boolean useExistingApplication, String wizardTitle) {
		setWindowTitle(wizardTitle);
		setNeedsProgressMonitor(true);
		this.model = new OpenShiftApplicationWizardModel(user, project, application,
				useExistingApplication);
		this.skipCredentialsPage = (user != null && user.isConnected());
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
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if (connection != null) {
			model.setConnection(connection);
		}
	}

	@Override
	public void addPages() {
		if (!skipCredentialsPage) {
			addPage(new ConnectionWizardPage(this, model));
		}
		addPage(new ApplicationConfigurationWizardPage(this, model));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, model));
		addPage(new GitCloningSettingsWizardPage(this, model));
	}

	@Override
	public boolean performFinish() {
		if (!model.isUseExistingApplication()) {

			IStatus status = createApplication();
			if (!handleOpenShiftError("creating the application", status)) {
				return false;
			}

			status = waitForApplication(model.getApplication());
			if (!handleOpenShiftError("waiting to become reachable", status)) {
				return false;
			}

			model.fireConnectionChanged();
		}

		if(!importProject()) {
			return false;
		}

		model.updateRecentConnection();

		if (!createServerAdapter()) {
			return false;
		}

		return publishServerAdapter();
	}

	private boolean handleOpenShiftError(String operation, IStatus status) {
		if (JobUtils.isCancel(status)) {
			if (AbstractDelegatingMonitorJob.TIMEOUTED == status.getCode()) {
				closeWizard();
			}
		}

		if (!JobUtils.isOk(status)) {
			safeRefreshUser();
			model.fireConnectionChanged();
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
	
	private boolean createServerAdapter() {
		if (!model.isCreateServerAdapter()) {
			return true;
		}
		IServer server = model.createServerAdapter(new DelegatingProgressMonitor());
		return server != null;
	}

	private boolean publishServerAdapter() {
		try {
			if (!EGitUtils.isDirty(model.getProject())) {
				IsAheadJob isAheadJob = new IsAheadJob(model.getProject(), model.getRemoteName());
				IStatus status =
						WizardUtils.runInWizard(isAheadJob, isAheadJob.getDelegatingProgressMonitor(), getContainer());
				if (!status.isOK()) {
					return false;
				}
				if (!isAheadJob.isAhead()) {
					return true;
				}
			}
			IStatus status = WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob(NLS.bind("Publishing project {0}...", model.getProjectName())) {
						
						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							return model.publishServerAdapter(monitor);
						}
					}, getContainer());
			return JobUtils.isOk(status);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error",
					NLS.bind("Could not publish project.", model.getProjectName()),
					OpenShiftUIActivator.createErrorStatus(
							"An exception occurred while publishing the server adapter.", e));
			return false;
		}
	}

	private IStatus createApplication() {
		try {
			CreateApplicationJob job = new CreateApplicationJob(
					model.getApplicationName()
					, model.getApplicationCartridge()
					, model.getApplicationScale()
					, model.getApplicationGearProfile()
					, model.getInitialGitUrl()
					, model.getSelectedEmbeddableCartridges()
					, model.getConnection().getDefaultDomain());
			IStatus status = WizardUtils.runInWizard(
					job, job.getDelegatingProgressMonitor(), getContainer(), APP_CREATE_TIMEOUT);
			IApplication application = job.getApplication();
			model.setApplication(application);
			if (status.isOK()) {
				openLogDialog(application, job.isTimeouted(status));
				openLogDialog(job.getAddedCartridges(), job.isTimeouted(status));
			}
			return status;
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Could not create application {0}", model.getApplicationName()), e);
		}
	}

	private void openLogDialog(final IApplication application, final boolean isTimeouted) {
		final LogEntry[] logEntries = LogEntryFactory.create(application, isTimeouted);
		if (logEntries.length == 0) {
			return;
		}
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), logEntries).open();
			}
		});
	}
	
	private void openLogDialog(final List<IEmbeddedCartridge> embeddableCartridges, final boolean isTimeouted) {
		final LogEntry[] logEntries = LogEntryFactory.create(embeddableCartridges, isTimeouted);
		if (logEntries == null
				|| logEntries.length == 0) {
			return;
		}

		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), logEntries).open();
			}
		});
	}

	private void safeRefreshUser() {
		try {
			model.getConnection().refresh();
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(e);
		}
	}

	OpenShiftApplicationWizardModel getModel() {
		return model;
	}
	
	@Override
	public void dispose() {
		model.dispose();
	}

	public boolean isCreateServerAdapter() {
		return model.isCreateServerAdapter();
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
				if (model.isNewProject()) {
					model.importProject(delegatingMonitor);
				} else if (!model.isGitSharedProject()) {
					if (!askForConfirmation(
							NLS.bind(
									"OpenShift application {0} will be enabled on project {1} by copying OpenShift configuration " +
									"from server to local project and connecting local project to OpenShift Git repository.\n" +
									"The local project will be automatically committed to local Git repository and further publishing will " +
									"eventually override existing remote content.\n\n" +
									"This cannot be undone. Do you wish to continue?",
									model.getApplicationName(), model.getProjectName()),
							model.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					model.mergeIntoUnsharedProject(delegatingMonitor);
				} else {
					if (!askForConfirmation(
							NLS.bind(
									"OpenShift application {0} will be enabled on project {1} by copying OpenShift configuration " +
									"from server to local project and connecting local project to OpenShift Git repository.\n" +
									"The local project will be automatically committed to local Git repository and further publishing will " +
									"eventually override existing remote content.\n\n" +
									"This cannot be undone. Do you wish to continue?",
									model.getApplicationName(), model.getProjectName()),
							model.getApplicationName())) {
						return Status.CANCEL_STATUS;
					}
					model.mergeIntoGitSharedProject(delegatingMonitor);
				}
				return Status.OK_STATUS;
			} catch (final WontOverwriteException e) {
				openError("Project already present", e.getMessage());
				return Status.CANCEL_STATUS;
			} catch (final ImportFailedException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not import project from application {0}.", e, model.getApplicationName());
			} catch (IOException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not copy openshift configuration files to project {0}", e, model
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

	}

	private class IsAheadJob extends AbstractDelegatingMonitorJob {

		private boolean isAhead = false;
		private CountDownLatch countdown = new CountDownLatch(1);
		private IProject project;
		private String remoteName;
		
		private IsAheadJob (IProject project, String remoteName) {
			super("Checking branch status");
			this.project = project;
			this.remoteName = remoteName;
		}
		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			try {
				isAhead = EGitUtils.isAhead(project, remoteName, monitor);
				countdown.countDown();
				return Status.OK_STATUS;
			} catch (Exception e) {
				return OpenShiftUIActivator.createErrorStatus("Could not check branch status", e);
			}
		}

		public boolean isAhead() throws InterruptedException {
			countdown.await();
			return isAhead;
		}
	}
}
