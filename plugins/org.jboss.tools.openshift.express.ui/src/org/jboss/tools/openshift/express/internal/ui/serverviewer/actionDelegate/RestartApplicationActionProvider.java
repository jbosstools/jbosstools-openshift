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
package org.jboss.tools.openshift.express.internal.ui.serverviewer.actionDelegate;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractOpenShiftAction;
import org.jboss.tools.openshift.express.internal.ui.job.RestartApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.RetrieveApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationActionProvider extends AbstractServerViewerActionProvider {

	public RestartApplicationActionProvider() {
		super(new RestartApplicationAction());
	}
	
	private static class RestartApplicationAction extends AbstractOpenShiftAction {

		public RestartApplicationAction() {
			super(OpenShiftExpressUIMessages.RESTART_APPLICATION_ACTION, true);
		}

		@Override
		public void validate() {
			boolean enabled = false;
			ISelection selection = getSelection();
			if (selection instanceof IStructuredSelection) {
				if (((IStructuredSelection) selection).size() == 1) {
					IServer server = UIUtils.getFirstElement(getSelection(), IServer.class);
					if (server != null) {
						String applicationName = ExpressServerUtils.getExpressApplicationId(server);
						if (!StringUtils.isEmpty(applicationName)) {
							enabled = true;
						}
					}
				}
			}
				
			setEnabled(enabled);
		}

		@Override
		public void run() {
			IServer server = UIUtils.getFirstElement(getSelection(), IServer.class);
			if (server == null) {
				return;
			}
			restartApplication(server);
		}

		protected void restartApplication(IServer server) {
			final RetrieveApplicationJob job = new RetrieveApplicationJob(server);
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					if (!event.getResult().isOK()) {
						return;
					}
					final IApplication application = job.getApplication();
					new RestartApplicationJob(application).schedule();
				}
			});
			job.schedule();
		}
	}
	
}
