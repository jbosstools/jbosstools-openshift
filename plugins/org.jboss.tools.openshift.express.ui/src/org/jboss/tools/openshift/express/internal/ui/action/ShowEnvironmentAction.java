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

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
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
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		try {
			final ITreeSelection treeSelection = (ITreeSelection) selection;
			if (selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
				final IApplication application = (IApplication) treeSelection.getFirstElement();
				if(!application.hasSSHSession()) {
					application.setSSHSession(OpenShiftSshSessionFactory.getInstance().createSession(application));
				}
				List<String> props = application.getEnvironmentProperties();
				final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName(application));
				MessageConsoleStream stream = console.newMessageStream();
				for (String prop : props) {
					stream.println(prop);
				}
				ConsoleUtils.displayConsoleView(console);
				
			}
		} catch (Exception e) {
			OpenShiftUIActivator.createErrorStatus("Failed to display remote environment variables", e);
		}

	}
	
	/**
	 * @return
	 */
	private String getMessageConsoleName(final IApplication application) {
		return "Environment Variables for application '" + application.getName() + "' (" + application.getDomain().getId() + ")";
	}


}
