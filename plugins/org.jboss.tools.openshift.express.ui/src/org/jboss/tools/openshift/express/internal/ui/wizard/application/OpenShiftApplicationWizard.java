/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.job.CreateApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.FireExpressConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.job.RefreshConnectionJob;
import org.jboss.tools.openshift.express.internal.ui.job.WaitForApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;
import org.jboss.tools.openshift.express.internal.ui.wizard.LogEntryFactory;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICodeAnythingApplicationTemplate;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * A wizard to import and create OpenShift applications. 
 * 
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * 
 * @see NewApplicationWorkbenchWizard
 * @see ImportOpenShiftApplicationWizard
 */
public abstract class OpenShiftApplicationWizard extends Wizard implements IConnectionAwareWizard<ExpressConnection> {

	private final OpenShiftApplicationWizardModel model;

	OpenShiftApplicationWizard(ExpressConnection connection, IDomain domain, IApplication application, IProject project, 
			boolean useExistingApplication, String wizardTitle) {
		setWindowTitle(wizardTitle);
		setNeedsProgressMonitor(true);
		this.model = new OpenShiftApplicationWizardModel(connection, domain, application, project, useExistingApplication);
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
	public void addPages() {
		addPage(new ApplicationTemplateWizardPage(this, model));
		addPage(new ApplicationConfigurationWizardPage(this, model));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, model));
		addPage(new GitCloningSettingsWizardPage(this, model));
	}

	@Override
	public boolean performFinish() {
			if (!model.isUseExistingApplication()) {

				IStatus status = createApplication();
				if (!handleOpenShiftError(
						NLS.bind("create application {0}", StringUtils.null2emptyString(model.getApplicationName())), status)) {
					return false;
				}

				status = waitForApplication(model.getApplication());
				if (!handleOpenShiftError(
						NLS.bind("wait for application {0} to become reachable", StringUtils.null2emptyString(model.getApplicationName())),
						status)) {
					return false;
				}

				new FireExpressConnectionsChangedJob(model.getConnection()).schedule();
				saveCodeAnythingUrl();
			}

			if (!importProject()) {
				return false;
			}

			if (!createServerAdapter()) {
				return false;
			}

			return true;
	}

	private boolean handleOpenShiftError(String operation, IStatus status) {
		if (JobUtils.isCancel(status)) {
			if (AbstractDelegatingMonitorJob.TIMEOUTED == status.getCode()) {
				closeWizard();
			}
		}

		if (!JobUtils.isOk(status)) {
			// dont open error-dialog, the jobs will do if they fail
			// ErrorDialog.openError(getShell(), "Error", "Could not " + operation, status);
			if (model.getConnection() != null) {
				new JobChainBuilder(new RefreshConnectionJob(model.getConnection()))
					.runWhenDone(new FireExpressConnectionsChangedJob(model.getConnection()))
					.schedule();
			}
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
					job, job.getDelegatingProgressMonitor(), getContainer());
			return status;
		} catch (Exception e) {
			return ExpressUIActivator.createErrorStatus(
					NLS.bind("Could not wait for application {0} to become reachable", application.getName()), e);
		}
	}

	private boolean importProject() {
		try {
			final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			IStatus jobResult = WizardUtils.runInWizard(
					new ImportJob(delegatingMonitor), delegatingMonitor, getContainer());
			return JobUtils.isOk(jobResult);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.", ExpressUIActivator
					.createErrorStatus("An exception occurred while creating local git repository.", e));
			return false;
		}
	}

	private void saveCodeAnythingUrl() {
		IApplicationTemplate template = model.getSelectedApplicationTemplate();
		if (!(template instanceof ICodeAnythingApplicationTemplate)) {
			return;
		}
		
		String url = ((ICodeAnythingApplicationTemplate) template).getUrl();
		ExpressCorePreferences.INSTANCE.addDownloadableStandaloneCartUrl(url);
	}
	
	private boolean createServerAdapter() {
		try {
			if (!model.isCreateServerAdapter()) {
				return true;
			}
			IServer server = model.createServerAdapter(new DelegatingProgressMonitor());
			return server != null;
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", NLS.bind("Could not create server adapter for new project {0}.",
					model.getProjectName()),
					ExpressUIActivator.createErrorStatus(e.getMessage(), e));
			return false;
		}
	}

	private IStatus createApplication() {
		try {
			CreateApplicationJob job = new CreateApplicationJob(
					model.getApplicationName()
					, model.getApplicationScale()
					, model.getApplicationGearProfile()
					, model.getInitialGitUrl()
					, model.getEnvironmentVariables()
					, model.getCartridges()
					, model.getDomain());
			IStatus status = WizardUtils.runInWizard(
					job, job.getDelegatingProgressMonitor(), getContainer());
			IApplication application = job.getApplication();
			model.setApplication(application);
			if (status.isOK()) {
				openLogDialog(application, job.isTimeouted(status));
				openLogDialog(job.getAddedCartridges(), job.isTimeouted(status));
			}
			return status;
		} catch (Exception e) {
			return ExpressUIActivator.createErrorStatus(
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
	private class ImportJob extends WorkspaceJob {

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
									"The local project will be committed to local Git repository upon confirmation and further publishing will " +
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
									"The local project will be committed to local Git repository upon confirmation and further publishing will " +
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
				return ExpressUIActivator.createErrorStatus(
						"Could not import project from application {0}.", e, model.getApplicationName());
			} catch (IOException e) {
				return ExpressUIActivator.createErrorStatus(
						"Could not copy openshift configuration files to project {0}", e, model
								.getProjectName());
			} catch (OpenShiftException e) {
				return ExpressUIActivator.createErrorStatus("Could not import project to the workspace.", e);
			} catch (URISyntaxException e) {
				return ExpressUIActivator.createErrorStatus("The url of the remote git repository is not valid", e);
			} catch (InvocationTargetException e) {
				TransportException te = getTransportException(e);
				if (te != null) {
					return ExpressUIActivator.createErrorStatus(
							"Could not clone the repository. Authentication failed.\n"
									+ " Please make sure that you added your private key to the ssh preferences.", te);
				} else {
					return ExpressUIActivator.createErrorStatus(
							"An exception occurred while creating local git repository.", e);
				}
			} catch (Exception e) {
				return ExpressUIActivator.createErrorStatus("Could not import project to the workspace.", e);
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

	@Override
	public ExpressConnection setConnection(ExpressConnection connection) {
		return model.setConnection(connection);
	}

	@Override
	public ExpressConnection getConnection() {
		return model.getConnection();
	}

	@Override
	public boolean hasConnection() {
		return model.hasConnection();
	}
}
