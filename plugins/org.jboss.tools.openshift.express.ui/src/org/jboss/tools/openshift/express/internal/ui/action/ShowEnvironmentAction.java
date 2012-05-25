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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftSshSessionFactory;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 */
public class ShowEnvironmentAction extends AbstractSSHAction {

	public ShowEnvironmentAction() {
		super(OpenShiftExpressUIMessages.SHOW_ENVIRONMENT_ACTION, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection) selection;
		if (selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) treeSelection.getFirstElement();
			showEnvironmentProperties(application);
		} else if (selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IServer) {
			final IServer server = (IServer) treeSelection.getFirstElement();
			showEnvironmentProperties(server);
		}

	}

	/**
	 * Retrieves the application from the given server, then opens the dialog.
	 * Since retrieving the application can be time consuming, the task is
	 * performed in a separate job (ie, in a background thread).
	 * 
	 * @param server
	 */
	private void showEnvironmentProperties(final IServer server) {
		Job job = new Job("Identifying OpenShift Application from selected Server...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final IApplication application = ExpressServerUtils.getApplication(server);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						showEnvironmentProperties(application);
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void showEnvironmentProperties(final IApplication application) {
			Job job = new Job("Retrieving selected OpenShift Application's environment variables...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					verifyApplicationSSHSession(application);
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							runAsync(application);
						}
					});
					return Status.OK_STATUS;
				} catch (OpenShiftSSHOperationException e) {
					return OpenShiftUIActivator.createErrorStatus(e.getMessage(), e.getCause());
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	/**
	 * @param application
	 */
	private void runAsync(final IApplication application) {
		try {
			if (!application.hasSSHSession()) {
				application.setSSHSession(OpenShiftSshSessionFactory.getInstance().createSession(application));
			}
			List<String> props = application.getEnvironmentProperties();
			final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName(application));
			console.clearConsole();
			MessageConsoleStream stream = console.newMessageStream();
			for (String prop : props) {
				stream.println(prop);
			}
			ConsoleUtils.displayConsoleView(console);
		} catch (Exception e) {
			OpenShiftUIActivator.createErrorStatus("Failed to display remote environment variables", e);
		}
	}

	/**
	 * @return
	 */
	private String getMessageConsoleName(final IApplication application) {
		return "Environment Variables for application '" + application.getName() + "' ("
				+ application.getDomain().getId() + ")";
	}

}
