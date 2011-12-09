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
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.common.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.common.WontOverwriteException;
import org.jboss.tools.openshift.express.internal.ui.wizard.AdapterWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.ApplicationWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPage;

/**
 * @author André Dietisheim
 */
public class ImportProjectWizard extends Wizard implements INewWizard {

	private ImportProjectWizardModel model;

	public ImportProjectWizard() {
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("OpenShift application wizard");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		try {
			final ArrayBlockingQueue<IStatus> queue = new ArrayBlockingQueue<IStatus>(1);
			WizardUtils.runInWizard(
					new WorkspaceJob("Importing project to workspace...") {

						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
							IStatus status = Status.OK_STATUS;
							status = performOperations(monitor, status);
							if (!status.isOK()) {
								OpenShiftUIActivator.log(status);
							}
							queue.offer(status);
							return status;
						}

						private IStatus performOperations(IProgressMonitor monitor, IStatus status) {
							try {
								if (model.isNewProject()) {
									model.importProject(monitor);
								} else {
									if (!askForConfirmation(model.getApplicationName(), model.getProjectName())) {
										return Status.CANCEL_STATUS;
									}
									model.addToExistingProject(monitor);
								}
								return Status.OK_STATUS;
							} catch (final WontOverwriteException e) {
								openWarning("Project already present", e.getMessage());
								return Status.CANCEL_STATUS;
							} catch (final ImportFailedException e) {
								return OpenShiftUIActivator.createErrorStatus(
										"Could not import maven project {0}.", e,
										model.getProjectName());
							} catch (IOException e) {
								return OpenShiftUIActivator.createErrorStatus(
										"Could not copy openshift configuration files to project {0}", e,
										model.getProjectName());
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
							}
						}
					}, getContainer());
			IStatus status = queue.poll(10, TimeUnit.SECONDS);
			return status != null
					&& status.isOK();
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.",
					new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							"An exception occurred while creating local git repository.", e));
			return false;
		}
	}

	private boolean askForConfirmation(final String applicationName, final String projectName) {
		final boolean[] confirmed = new boolean[1]; 
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				confirmed[0] = MessageDialog.openConfirm(getShell(), 
						NLS.bind("Import OpenShift Application ", applicationName), 
						NLS.bind(
								"OpenShift application {0} will be enabled on project {1} by copying OpenShift " +
								"configuration and enable Git for the project.\n " +
						"This cannot be undone. Do you wish to continue ?", applicationName, projectName));
			}});
		return confirmed[0]; 
	}

	private void openWarning(final String title, final String message) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openWarning(getShell(), title, message);
			}
		});
	}

	@Override
	public void addPages() {
		this.model = new ImportProjectWizardModel();
		addPage(new CredentialsWizardPage(this, model));
		addPage(new ApplicationWizardPage(this, model));
		addPage(new AdapterWizardPage(this, model));
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
}
