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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.CreateSSHSessionJob;
import org.jboss.tools.openshift.express.internal.ui.portforward.PortForwardingWizard;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;

import com.openshift.client.IApplication;

public class PortForwardingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IApplication application = UIUtils.getFirstElement(selection, IApplication.class);
		if (application != null) {
			openPortForwardingDialogFor(application);
		} else {
			IServer server = UIUtils.getFirstElement(selection, IServer.class);
			if (server != null) {
				openPortForwardingDialogFor(server);
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus openPortForwardingDialogFor(final IApplication application) {
		final CreateSSHSessionJob sshJob = new CreateSSHSessionJob(application);
		new JobChainBuilder(sshJob)
				.runWhenSuccessfullyDone(new UIJob("Opening port forwarding dialog") {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (sshJob.isValidSession()) {
							openPortForwardingWizard(application);
						}
						return Status.OK_STATUS;
					}
				}).schedule();
		return Status.OK_STATUS;
	}

	private void openPortForwardingDialogFor(final IServer server) {
		final LoadApplicationJob applicationJob = new LoadApplicationJob(server);
		final CreateSSHSessionJob sshJob = new CreateSSHSessionJob(applicationJob);
		new JobChainBuilder(applicationJob)
				.runWhenSuccessfullyDone(sshJob)
				.runWhenSuccessfullyDone(new UIJob("Opening port forwarding dialog") {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						IApplication application = applicationJob.getApplication();
						if (application != null
								&& sshJob.isValidSession()) {
							openPortForwardingWizard(application);
						}
						return Status.OK_STATUS;
					}
				}).schedule();
	}

	private void openPortForwardingWizard(IApplication application) {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		WizardDialog dialog = new OkButtonWizardDialog(shell, new PortForwardingWizard(application));
		dialog.setMinimumPageSize(700, 400);
		dialog.create();
		dialog.open();
	}
}
