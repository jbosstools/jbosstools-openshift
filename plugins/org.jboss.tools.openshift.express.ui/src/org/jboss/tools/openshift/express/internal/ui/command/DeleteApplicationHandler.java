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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.job.DeleteApplicationsJob;
import org.jboss.tools.openshift.express.internal.ui.job.RefreshConnectionsModelJob;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class DeleteApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<IApplication> appsToDelete = getApplicationsToDelete(HandlerUtil.getCurrentSelection(event));
		if (appsToDelete == null
				|| appsToDelete.size() == 0) {
			return null;
		}

		if (promptForConfirmation(appsToDelete, HandlerUtil.getActiveShell(event))) {
			List<IUser> users = getUsers(appsToDelete);

			new JobChainBuilder(new DeleteApplicationsJob(appsToDelete))
					.andRunWhenDone(new RefreshConnectionsModelJob(users))
					.build()
					.schedule();
		}
		return Status.OK_STATUS;
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

	private boolean promptForConfirmation(final List<IApplication> appsToDelete, Shell shell) {
		if (appsToDelete.size() == 1) {
			return MessageDialog.openConfirm(shell,
					"Application deletion",
					NLS.bind(
							"You are about to destroy the \"{0}\" application.\n"
									+ "This is NOT reversible, all remote data for this application will be removed.",
							appsToDelete.get(0).getName()));
		} else if (appsToDelete.size() > 1) {
			return MessageDialog.openConfirm(shell,
					"Application deletion",
					NLS.bind("You are about to destroy {0} applications.\n"
							+ "This is NOT reversible, all remote data for those applications will be removed.",
							appsToDelete.size()));
		}
		return false;
	}

	private List<IApplication> getApplicationsToDelete(ISelection selection) {
		final List<IApplication> appsToDelete = new ArrayList<IApplication>();
		if (!(selection instanceof IStructuredSelection)) {
			return appsToDelete;
		}
		for (@SuppressWarnings("unchecked")
		Iterator<Object> iterator = ((IStructuredSelection) selection).iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			if (isApplication(element)) {
				appsToDelete.add((IApplication) element);
			}
		}
		return appsToDelete;
	}

	private boolean isApplication(Object selection) {
		return selection instanceof IApplication;
	}
}
