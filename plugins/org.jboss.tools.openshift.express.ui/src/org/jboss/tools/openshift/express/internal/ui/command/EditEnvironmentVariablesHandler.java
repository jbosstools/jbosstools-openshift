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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.environment.EditEnvironmentVariablesWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class EditEnvironmentVariablesHandler extends AbstractDomainHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IApplication application = UIUtils.getFirstElement(selection, IApplication.class);
		if (application != null) {
			return openEnvironmentVariablesWizard(application, HandlerUtil.getActiveShell(event));
		} else {
			IServer server = UIUtils.getFirstElement(selection, IServer.class);
			if (server == null) {
				return null;
			}
			return openEnvironmentVariablesWizard(server, HandlerUtil.getActiveShell(event));
		}
	}

	private Object openEnvironmentVariablesWizard(IApplication application, Shell shell) {
		try {
			WizardUtils.openWizardDialog(
					new EditEnvironmentVariablesWizard(application), shell);
			return null;
		} catch (OpenShiftException e) {
			Logger.error("Failed to edit cartridges", e);
			return ExpressUIActivator.createErrorStatus("Failed to edit cartridges", e);
		}
	}

	private Object openEnvironmentVariablesWizard(IServer server, final Shell shell) {
		final LoadApplicationJob job = new LoadApplicationJob(server);
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (!event.getJob().getResult().isOK()) {
					return;
				}
				final IApplication application = job.getApplication();
				if (application == null) {
					return;
				}
				shell.getDisplay().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						openEnvironmentVariablesWizard(application, shell);
					}
				});
			}
		});
		job.schedule();
		return null;
	}

}
