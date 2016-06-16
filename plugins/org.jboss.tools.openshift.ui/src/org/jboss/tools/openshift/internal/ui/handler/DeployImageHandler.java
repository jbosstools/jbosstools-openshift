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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
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
		IWorkbenchWindow window = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow();
		ISelection selection = window.getSelectionService().getSelection();

		IProject project = null;
		Connection connection = null;

		boolean checkConnection = true;
		final IDockerImage image = UIUtils.getFirstElement(selection, IDockerImage.class);
		if(image != null) {
			checkConnection = false;
			//Look for current selection in OpenShift Explorer
			IViewPart part = window.getActivePage().findView(OpenShiftUIUtils.OPENSHIFT_EXPLORER_VIEW_ID);
			if(part != null) {
				selection = part.getSite().getSelectionProvider().getSelection();
				if(selection != null && !selection.isEmpty()) {
					checkConnection = true;
				}
			}
		}
		if(checkConnection) {
			project = UIUtils.getFirstElement(selection, IProject.class);
			if(project == null) {
				//If another resource is selected in OpenShift explorer, navigate to its project.
				IResource resource = UIUtils.getFirstElement(selection, IResource.class);
				if (resource != null) {
					project= resource.getProject();
				}
			}
			if(project != null) {
				connection = ConnectionsRegistryUtil.getConnectionFor(project);
			} else {
				connection = UIUtils.getFirstElement(selection, Connection.class);
			}
		}

		if(connection == null) {
			connection = OpenShiftUIUtils.getDefaultConnection(Connection.class);
		}

		runWizard(HandlerUtil.getActiveWorkbenchWindow(event).getShell(), image, connection, project);

		return null;
	}

	public void runWizard(final Shell shell, final IDockerImage image, final Connection connection, final IProject project) {
		if(connection != null) {
			final boolean[] connected = new boolean[1];
			Job job = new AbstractDelegatingMonitorJob("Checking connection...") {
				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						connected [0] = connection.isConnected(new NullProgressMonitor());
						return Status.OK_STATUS;
					}catch(Exception e) {
						return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Unable to load the OpenShift projects for the selected connection.", e);
					}
				}
			};
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					shell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							DeployImageWizard wizard = new DeployImageWizard(image, connection, project, connected[0]);
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
