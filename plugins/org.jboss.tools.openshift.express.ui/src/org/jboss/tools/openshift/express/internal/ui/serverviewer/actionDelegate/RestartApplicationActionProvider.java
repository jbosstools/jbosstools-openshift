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

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.action.AbstractOpenShiftAction;
import org.jboss.tools.openshift.express.internal.ui.job.RestartApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.RetrieveApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

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
		public void run() {
			IServer server = UIUtils.getFirstElement(getSelection(), IServer.class);
			if (server == null) {
				return;
			}
			restartApplication(server);
		}

		protected void restartApplication(IServer server) {
			final RetrieveApplicationJob retrieveAppliaction = new RetrieveApplicationJob(server);
			new JobChainBuilder(retrieveAppliaction)
					.andRunWhenSuccessfull(new RestartApplicationJob(retrieveAppliaction.getApplication()))
					.build().schedule();
		}
	}
	
}
