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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.snapshot.SaveSnapshotWizard;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class SaveSnapshotHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application != null) {
			// explorer
			openSaveSnapshotWizard(application, HandlerUtil.getActiveShell(event));
		} else {
			// servers view
			IServer server = (IServer)
					UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			if (server == null) {
				return ExpressUIActivator.createErrorStatus("Could not find application to snapshot");
			}
			final LoadApplicationJob loadApplicationJob = new LoadApplicationJob(server);
			new JobChainBuilder(loadApplicationJob)
					.runWhenSuccessfullyDone(new UIJob("Opening Save Snapshot wizard...") {

						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							IApplication application = loadApplicationJob.getApplication();
							if (application == null) {
								return ExpressUIActivator
										.createCancelStatus("Could not find application to save the snapshot for.");
							}
							openSaveSnapshotWizard(loadApplicationJob.getApplication(),
									HandlerUtil.getActiveShell(event));
							return Status.OK_STATUS;
						}
					})
					.schedule();
			return Status.OK_STATUS;
		}
		return Status.OK_STATUS;
	}

	private void openSaveSnapshotWizard(IApplication application, Shell shell) {
		WizardUtils.openWizard(
				new SaveSnapshotWizard(application), shell);
	}

}
