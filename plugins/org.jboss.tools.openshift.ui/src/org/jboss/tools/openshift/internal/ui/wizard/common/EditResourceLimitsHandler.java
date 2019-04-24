/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.handler.EditResourceLimitsPage;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * Handler for editing resource limits
 */
public class EditResourceLimitsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource resource = UIUtils.getFirstElement(UIUtils.getCurrentSelection(event), IResource.class);
		RetrieveDCOrRCJob job = new RetrieveDCOrRCJob(resource);
		new JobChainBuilder(job).runWhenSuccessfullyDone(new UIJob("Launching Edit Resource Limits Wizard...") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IReplicationController dcOrRc = job.getDeplConfigOrReplController();
				if (dcOrRc == null) {
					return Status.CANCEL_STATUS;
				}
				editResources(HandlerUtil.getActiveShell(event), dcOrRc);
				return Status.OK_STATUS;
			}
		}).schedule();
		return null;
	}

	protected void editResources(Shell shell, IReplicationController rc) {
		EditResourceLimitsPageModel model = new EditResourceLimitsPageModel(rc);
		EditResourceLimitsWizard wizard = new EditResourceLimitsWizard(model, "Edit resource limits");
		new OkCancelButtonWizardDialog(shell, wizard).open();
	}

	class EditResourceLimitsWizard extends Wizard {
		private EditResourceLimitsPageModel model;

		public EditResourceLimitsWizard(EditResourceLimitsPageModel model, String title) {
			this.model = model;
			setWindowTitle(title);
		}

		@Override
		public void addPages() {
			addPage(new EditResourceLimitsPage(model, this));
		}

		@Override
		public boolean performFinish() {
			model.dispose();
			new Job(NLS.bind(OpenShiftUIMessages.EditResourceLimitsJobTitle,
					model.getUpdatedReplicationController().getName())) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						IReplicationController rc = model.getUpdatedReplicationController();
						Connection connection = ConnectionsRegistryUtil.getConnectionFor(rc);
						connection.updateResource(rc);
						return Status.OK_STATUS;
					} catch (Exception e) {
						String message = NLS.bind(OpenShiftUIMessages.EditResourceLimitsJobErrorMessage,
								model.getUpdatedReplicationController().getName());
						OpenShiftUIActivator.getDefault().getLogger().logError(message, e);
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, e);
					}
				}
			}.schedule();
			return true;
		}
	}

	private class RetrieveDCOrRCJob extends Job {

		private final IResource resource;
		private IReplicationController deploymentConfOrReplController;

		public RetrieveDCOrRCJob(IResource resource) {
			super("Retrieve Deployment Config or Replication Controller...");
			this.resource = resource;
			this.deploymentConfOrReplController = null;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			this.deploymentConfOrReplController = getDeploymentConfigOrReplicationController(resource);
			if (deploymentConfOrReplController == null) {
				return OpenShiftUIActivator.statusFactory()
						.errorStatus(NLS.bind(
								"Could not edit resources {0}: Could not find deployment config or replication controller",
								resource == null ? "" : resource.getName()));
			}
			return Status.OK_STATUS;
		}

		/**
		 * Gets a deployment config or a replication controller (if it can't find a
		 * deployment config) for the given resource.
		 * 
		 * @param resource the resource to get a dc or rc for
		 * @return
		 */
		private IReplicationController getDeploymentConfigOrReplicationController(final IResource resource) {
			// TODO: move to ResourceUtils
			if (resource == null) {
				return null;
			}

			Connection connection = ConnectionsRegistryUtil.getConnectionFor(resource);
			IReplicationController dcOrRc = ResourceUtils.getDeploymentConfigFor(resource, connection);
			if (null == dcOrRc) {
				if (resource instanceof IService) {
					dcOrRc = ResourceUtils.getReplicationControllerFor((IService) resource,
							resource.getProject().getResources(ResourceKind.REPLICATION_CONTROLLER));
				} else if (resource instanceof IReplicationController) {
					dcOrRc = (IReplicationController) resource;
				} else if (resource instanceof IPod) {
					dcOrRc = ResourceUtils.getDeploymentConfigOrReplicationControllerFor((IPod) resource);
				}
			}
			return dcOrRc;
		}

		IReplicationController getDeplConfigOrReplController() {
			return deploymentConfOrReplController;
		}

	}

}
