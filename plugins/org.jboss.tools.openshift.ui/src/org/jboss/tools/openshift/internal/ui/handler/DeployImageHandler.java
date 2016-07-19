/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizard;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;


/**
 * @author jeff.cantrill
 */
public class DeployImageHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Connection connection = null;
		IProject project = null;
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		final IDockerImage image = UIUtils.getFirstElement(selection, IDockerImage.class);
		if (image == null
				|| OpenShiftUIUtils.hasOpenShiftExplorerSelection()) {
			selection = OpenShiftUIUtils.getOpenShiftExplorerSelection();
			project = ResourceUtils.getProject(UIUtils.getFirstElement(selection, IResource.class));
			if(project != null) {
				connection = ConnectionsRegistryUtil.getConnectionFor(project);
			} else {
				connection = UIUtils.getFirstElement(selection, Connection.class);
			}
		}

		if(connection == null) {
			connection = OpenShiftUIUtils.getExplorerDefaultConnection(Connection.class);
		}

		runWizard(HandlerUtil.getActiveWorkbenchWindow(event).getShell(), image, project,connection);

		return null;
	}

	public void runWizard(final Shell shell, final IDockerImage image, final IProject project, final Connection connection) {
		if(connection != null) {
			final boolean[] authorized = new boolean[1];
			Job job = new AbstractDelegatingMonitorJob("Checking connection...") {
				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						authorized [0] = connection.isAuthorized(new NullProgressMonitor());
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Unable to load the OpenShift projects for the selected connection.", e);
					}
				}
			};
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					shell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							DeployImageWizard wizard = new DeployImageWizard(image, connection, project, authorized[0]);
							WizardUtils.openWizardDialog(500, 500, wizard, shell);
						}
					});
				}
			});
			job.schedule();
		} else {
			DeployImageWizard wizard = new DeployImageWizard(image, connection, project, false);
			WizardUtils.openWizardDialog(600, 1500, wizard, shell);
		}
	}

}
