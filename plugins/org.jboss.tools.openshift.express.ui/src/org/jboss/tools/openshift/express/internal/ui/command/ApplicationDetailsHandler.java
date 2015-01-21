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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.details.ApplicationDetailsDialog;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class ApplicationDetailsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (application != null) {
			openApplicationDetailsDialog(application, shell);
		} else {
			IServer server = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			openApplicationDetailsDialog(server, shell);
		}
		return Status.OK_STATUS;
	}

	protected void openApplicationDetailsDialog(final IServer server, final Shell shell) {
		if (server == null) {
			return;
		}
		final LoadApplicationJob applicationJob = new LoadApplicationJob(server);
		new JobChainBuilder(applicationJob)
			.runWhenSuccessfullyDone(new UIJob(NLS.bind("Displaying application details", server.getName())) {
				
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IApplication application = applicationJob.getApplication();
					if (application == null) {
						return ExpressUIActivator.createCancelStatus("Could not display details for application {0}. Application not found.", server.getName());
					}
					openApplicationDetailsDialog(application, shell);
					return Status.OK_STATUS;
				}
			})
			.schedule();
	}

	protected void openApplicationDetailsDialog(IApplication application, Shell shell) {
		new ApplicationDetailsDialog(application, shell).open();
	}
}
