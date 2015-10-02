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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.server.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.internal.client.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractApplicationHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (application != null) {
			return execute(application, shell);
		} else {
			IServer server = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			if (server == null) {
				return ExpressUIActivator.createCancelStatus(NLS.bind("Could not {0}: server adapter not found.", getOperationName()));
			}
			return execute(server, shell);
		}
	}

	protected IStatus execute(IServer server, Shell shell) {
		String applicationName = ExpressServerUtils.getApplicationName(server);
		if (StringUtils.isEmpty(applicationName)) {
			return ExpressUIActivator.createCancelStatus(NLS.bind(
					"Could not {0} application: application for server adapter {1} not found.",
					getOperationName(),
					server.getName()));
		}
		return execute(new LoadApplicationJob(server), shell);
	}
	
	protected abstract IStatus execute(IApplication application, Shell shell);

	protected abstract IStatus execute(LoadApplicationJob loadApplicationJob, Shell shell);

	protected abstract String getOperationName();
}
