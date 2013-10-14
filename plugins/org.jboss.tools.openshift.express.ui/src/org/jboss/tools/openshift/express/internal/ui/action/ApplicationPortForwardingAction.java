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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.ui.job.RetrieveApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.VerifySSHSessionJob;
import org.jboss.tools.openshift.express.internal.ui.portforward.ApplicationPortForwardingWizard;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;

import com.openshift.client.IApplication;

public class ApplicationPortForwardingAction extends AbstractOpenShiftAction {

	public ApplicationPortForwardingAction() {
		super("Port forwarding...", DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no
	 * Console/Worker existed, a new one is created, otherwise, it is displayed.
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		IApplication application = UIUtils.getFirstElement(getSelection(), IApplication.class);
		if (application != null) {
			openPortForwardingDialogFor(application);
		} else {
			IServer server = UIUtils.getFirstElement(getSelection(), IServer.class);
			if (server != null) {
				openPortForwardingDialogFor(server);
			}
		}
	}

	/**
	 * Retrieves the application from the given server, then opens the dialog.
	 * Since retrieving the application can be time consuming, the task is
	 * performed in a separate job (ie, in a background thread).
	 * 
	 * @param server
	 */
	private void openPortForwardingDialogFor(final IServer server) {
		final RetrieveApplicationJob job = new RetrieveApplicationJob(server);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					final IApplication application = job.getApplication();
					openPortForwardingDialogFor(application);
				}
			}
		});
		job.setUser(true);
		job.schedule();
	}

	private void openPortForwardingDialogFor(final IApplication application) {
		final VerifySSHSessionJob job = new VerifySSHSessionJob(application);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK() && job.isValidSession()) {
					openWizardDialog(application);
				}
			}
		});

		job.setUser(true);
		job.schedule();
	}

	private void openWizardDialog(final IApplication application) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
				WizardDialog dialog = new OkButtonWizardDialog(shell, new ApplicationPortForwardingWizard(application));
				dialog.setMinimumPageSize(700, 300);
				dialog.create();
				dialog.open();
			}
		});
	}
}
