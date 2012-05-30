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

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.job.RetrieveApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.VerifySSHSessionJob;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftSshSessionFactory;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class ShowEnvironmentAction extends AbstractAction {

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
			showEnvironmentPropertiesFor(application);
		} else if (selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IServer) {
			final IServer server = (IServer) treeSelection.getFirstElement();
			showEnvironmentPropertiesFor(server);
		}

	}

	/**
	 * Retrieves the application from the given server, then opens the dialog.
	 * Since retrieving the application can be time consuming, the task is
	 * performed in a separate job (ie, in a background thread).
	 * 
	 * @param server
	 */
	private void showEnvironmentPropertiesFor(final IServer server) {
		final RetrieveApplicationJob job = new RetrieveApplicationJob(server);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					final IApplication application = job.getApplication();
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							showEnvironmentPropertiesFor(application);
						}
					});
				}
			}
		});
		job.setUser(true);
		job.schedule();
	}

	private void showEnvironmentPropertiesFor(final IApplication application) {
		final VerifySSHSessionJob job = new VerifySSHSessionJob(application);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if(event.getResult().isOK() && job.isValidSession()) {
					showEnvironmentProperties(application);
				}
			}
		});
		
		job.setUser(true);
		job.schedule();
	}
	
	/**
	 * @param application
	 */
	private void showEnvironmentProperties(final IApplication application) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
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
		});
	}

	/**
	 * @return
	 */
	private String getMessageConsoleName(final IApplication application) {
		return "Environment Variables for application '" + application.getName() + "' ("
				+ application.getDomain().getId() + ")";
	}

}
