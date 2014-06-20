/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.RestartApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.internal.client.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (application != null) {
			return restartFor(application, shell);
		} else {
			IServer server = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			if (server == null) {
				return OpenShiftUIActivator.createCancelStatus("Could not restart application: server not found.");
			}
			return restartFor(server, shell);
		}
	}

	private Object restartFor(IApplication application, Shell shell) {
		if (!promptUserToConfirm(application.getName(), shell)) {
			return OpenShiftUIActivator.createCancelStatus(
					"Restarting application {0} was cancelled.", application.getName());
		}
		new RestartApplicationJob(application).schedule();
		return Status.OK_STATUS;
	}

	private IStatus restartFor(IServer server, Shell shell) {
		String applicationName = OpenShiftServerUtils.getApplicationName(server);
		if (StringUtils.isEmpty(applicationName)) {
			return OpenShiftUIActivator.createCancelStatus(NLS.bind(
					"Could not restart application: application for server {0} not found.",
					server.getName()));
		}
		if (!promptUserToConfirm(applicationName, shell)) {
			return OpenShiftUIActivator.createCancelStatus(
					"Restarting application {0} was cancelled.", applicationName);
		}
		final LoadApplicationJob applicationJob = new LoadApplicationJob(server);
		new JobChainBuilder(applicationJob)
				.runWhenSuccessfullyDone(new RestartApplicationJob(applicationJob)).schedule();
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
}
