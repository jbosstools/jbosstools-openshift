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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.ImportFailedException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.WontOverwriteException;

import com.openshift.express.client.OpenShiftException;

/**
 * @author Andr� Dietisheim
 * @author Xavier Coulon
 */
public class ImportExistingApplicationWizard extends AbstractOpenShiftApplicationWizard<ImportExistingApplicationWizardModel> implements IImportWizard {

	public ImportExistingApplicationWizard() {
		super();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("OpenShift application wizard");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		try {
			final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			IStatus jobResult =
					WizardUtils.runInWizard(
							new ImportJob(delegatingMonitor),
							delegatingMonitor, getContainer());
			return JobUtils.isOk(jobResult);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "Could not create local git repository.",
					new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							"An exception occurred while creating local git repository.", e));
			return false;
		}
	}

	@Override
	public void addPages() {
		setWizardModel(new ImportExistingApplicationWizardModel());
		addPage(new CredentialsWizardPage(this, getWizardModel()));
		addPage(new ApplicationSelectionWizardPage(this, getWizardModel()));
		addPage(new ProjectAndServerAdapterSettingsWizardPage(this, getWizardModel()));
		addPage(new GitCloningSettingsWizardPage(this, getWizardModel()));
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

				if (getWizardModel().isNewProject()) {
					getWizardModel().importProject(delegatingMonitor);
				} else {
					if (!askForConfirmation(getWizardModel().getApplicationName(), getWizardModel().getProjectName())) {
						return Status.CANCEL_STATUS;
					}
					getWizardModel().configureUnsharedProject(delegatingMonitor);
				}

				return Status.OK_STATUS;
			} catch (final WontOverwriteException e) {
				openError("Project already present", e.getMessage());
				return Status.CANCEL_STATUS;
			} catch (final ImportFailedException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not import maven project {0}.", e,
						getWizardModel().getProjectName());
			} catch (IOException e) {
				return OpenShiftUIActivator.createErrorStatus(
						"Could not copy openshift configuration files to project {0}", e,
						getWizardModel().getProjectName());
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
