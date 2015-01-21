/*******************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.RestartApplicationJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationHandler extends AbstractApplicationHandler {

	@Override
	protected IStatus execute(IApplication application, Shell shell) {
		if (!promptUserToConfirm(application.getName(), shell)) {
			return ExpressUIActivator.createCancelStatus(
					"Restarting application {0} was cancelled.", application.getName());
		}
		new RestartApplicationJob(application).schedule();
		return Status.OK_STATUS;
	}

	@Override
	protected IStatus execute(LoadApplicationJob job, Shell shell) {
		if (!promptUserToConfirm(job.getApplicationName(), shell)) {
			return ExpressUIActivator.createCancelStatus(
					"Restarting application {0} was cancelled.", job.getApplicationName());
		}
		new JobChainBuilder(job)
				.runWhenSuccessfullyDone(new RestartApplicationJob(job)).schedule();
		return Status.OK_STATUS;
	}

	private boolean promptUserToConfirm(String applicationName, Shell shell) {
		return MessageDialog
				.openQuestion(
						shell,
						"Restart Application",
						NLS.bind("You are about to restart application {0}.\n\n"
								+ "Restarting an application in production may be harmful. "
								+ "Are you sure that you want to restart your application?",
								applicationName));
	}

	@Override
	protected String getOperationName() {
		return "restart application";
	}
}
