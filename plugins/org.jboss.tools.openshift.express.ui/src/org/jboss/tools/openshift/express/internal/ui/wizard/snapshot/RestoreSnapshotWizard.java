/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.snapshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;

import com.openshift.client.DeploymentTypes;
import com.openshift.client.IApplication;
import com.openshift.internal.client.utils.StringUtils;

/**
 * @author André Dietisheim
 */
public class RestoreSnapshotWizard extends AbstractOpenShiftWizard<RestoreSnapshotWizardModel> {

	public RestoreSnapshotWizard(IApplication application) {
		super("Restore/Deploy Snapshot", new RestoreSnapshotWizardModel(application));
	}

	@Override
	public boolean performFinish() {
		final IApplication application = getModel().getApplication();
		final String applicationName = application.getName();
		try {
			final RestoreJob restoreJob = new RestoreJob(application);
			Job jobChain = new JobChainBuilder(restoreJob)
					.runWhenDone(
							new UIJob(NLS.bind(
									"Show Snapshot Restore/Deploy Output for application {0}...",
									applicationName)) {

								@Override
								public IStatus runInUIThread(IProgressMonitor monitor) {
									MessageConsole console = ConsoleUtils.displayConsoleView(application);
									if (console == null) {
										return ExpressUIActivator.createCancelStatus(NLS.bind(
												"Cound not open console for application {0}", applicationName));
									}
									printResponse(restoreJob.getResponse(), console);
									return Status.OK_STATUS;
								}

								private void printResponse(String response, MessageConsole console) {
									MessageConsoleStream messageStream = console.newMessageStream();
									if (StringUtils.isEmpty(response)) {
										messageStream.print("Done");
									} else {
										messageStream.print(response);
									}
								}
							})
					.build();
			WizardUtils.runInWizard(jobChain, getContainer());
			return restoreJob.getResult().isOK();
		} catch (InvocationTargetException e) {
			IStatus status = ExpressUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not restore snapshot for application {0}", applicationName),
					status, IStatus.ERROR)
					.open();
			return false;
		} catch (InterruptedException e) {
			IStatus status = ExpressUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not restore snapshot for application {0}", applicationName),
					status, IStatus.ERROR)
					.open();
			return false;
		}
	}

	@Override
	public void addPages() {
		addPage(new RestoreSnapshotWizardPage(getModel(), this));
	}

	private class RestoreJob extends AbstractDelegatingMonitorJob {

		private IApplication application;
		private String response;

		RestoreJob(IApplication application) {
			super(NLS.bind("Restoring/Deploying snapshot for application {0}...", application.getName()));
			this.application = application;
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			String deploymentType = application.getDeploymentType();
			try {
				if (getModel().isDeploymentSnapshot()) {
					// set binary deployment type for deployment snapshots
					monitor.subTask(NLS.bind("Setting binary deployment type for application {0}...", application.getName()));
					application.setDeploymentType(DeploymentTypes.binary());
				}
				monitor.subTask("Restoring snapshot...");
				this.response = getModel().restoreSnapshot(monitor);
				return Status.OK_STATUS;
			} catch (IOException e) {
				return ExpressUIActivator.createErrorStatus(
						NLS.bind("Could not restore snapshot for application {0}", application.getName()), e);
			} finally {
				if (getModel().isDeploymentSnapshot()) {
					// restore
					monitor.subTask(NLS.bind("Restoring deployment type {0} for application {1}", deploymentType, application.getName()));
					application.setDeploymentType(deploymentType);
				}
			}
		}

		public String getResponse() {
			return response;
		}
	}
}
