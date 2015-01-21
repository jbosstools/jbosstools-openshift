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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.DeleteApplicationsJob;
import org.jboss.tools.openshift.express.internal.ui.job.FireExpressConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class DeleteApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		final List<IApplication> appsToDelete = getApplicationsToDelete(HandlerUtil.getCurrentSelection(event));
		if (appsToDelete != null
				&& !appsToDelete.isEmpty()) {
			return deleteApplications(appsToDelete, shell);
		} else {
			IServer server = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			if (server == null) {
				return ExpressUIActivator.createCancelStatus("Could not find the server adapter to delete");
			}
			return deleteApplicationAndServer(server, shell);
		}
	}

	private IStatus deleteApplications(final List<IApplication> appsToDelete, Shell shell) {
		if (promptForDeleteConfirmation(appsToDelete, shell)) {
			List<IUser> users = getUsers(appsToDelete);
			new JobChainBuilder(new DeleteApplicationsJob(appsToDelete))
					.runWhenDone(new FireExpressConnectionsChangedJob(users))
					.schedule();
			return Status.OK_STATUS;
		} else {
			return ExpressUIActivator.createCancelStatus("Cancelled application removal.");
		}
	}

	private List<IUser> getUsers(List<IApplication> applications) {
		List<IUser> users = new ArrayList<IUser>();
		if (applications == null
				|| applications.size() == 0) {
			return users;
		}

		for (IApplication application : applications) {
			users.add(application.getDomain().getUser());
		}
		return users;
	}

	private boolean promptForDeleteConfirmation(final List<IApplication> appsToDelete, Shell shell) {
		if (appsToDelete.size() == 1) {
			return MessageDialog.openConfirm(shell,
					"Application removal",
					NLS.bind(
							"You are about to destroy the \"{0}\" application.\n"
									+ "This is NOT reversible, all remote data for this application will be removed.",
							appsToDelete.get(0).getName()));
		} else if (appsToDelete.size() > 1) {
			return MessageDialog.openConfirm(shell,
					"Application removal",
					NLS.bind("You are about to destroy {0} applications.\n"
							+ "This is NOT reversible, all remote data for those applications will be removed.",
							appsToDelete.size()));
		}
		return false;
	}

	private IStatus deleteApplicationAndServer(final IServer server, Shell shell) {
		if (MessageDialog
				.openConfirm(
						shell,
						"Application and Server removal",
						NLS.bind(
								"You are about to remove the application and the server adapter \"{0}\".\n"
										+ "This is NOT reversible, all remote data for this application and the local server adapter will be removed.",
								server.getName()))) {
			LoadApplicationJob applicationJob = new LoadApplicationJob(server);
			new JobChainBuilder(applicationJob)
					.runWhenSuccessfullyDone(
							new DeleteApplicationsJob(applicationJob))
					.runWhenSuccessfullyDone(new FireExpressConnectionsChangedJob(applicationJob))
					.runWhenSuccessfullyDone(new AbstractDelegatingMonitorJob(NLS.bind("Delete Server Adapter {0}", server.getName())) {
						
						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							try {
								server.delete();
								return Status.OK_STATUS;
							} catch (CoreException e) {
								return e.getStatus();
							}
						}
					})
					.schedule();
		}
		;
		return Status.OK_STATUS;

	}

	private List<IApplication> getApplicationsToDelete(ISelection selection) {
		final List<IApplication> appsToDelete = new ArrayList<IApplication>();
		if (!(selection instanceof IStructuredSelection)) {
			return appsToDelete;
		}
		for (@SuppressWarnings("unchecked")
		Iterator<Object> iterator = ((IStructuredSelection) selection).iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			if (element instanceof IApplication) {
				appsToDelete.add((IApplication) element);
			}
		}
		return appsToDelete;
	}
}
