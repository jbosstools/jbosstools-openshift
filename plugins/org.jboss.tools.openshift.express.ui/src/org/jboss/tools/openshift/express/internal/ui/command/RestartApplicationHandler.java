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
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.job.RestartApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application != null) {
			new RestartApplicationJob(application).schedule();
			return Status.OK_STATUS;
		} else { 
			IServer server = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IServer.class);
			LoadApplicationJob applicationJob = new LoadApplicationJob(server);
			new JobChainBuilder(applicationJob)
				.runWhenSuccessfullyDone(new RestartApplicationJob(applicationJob)).schedule();
		}
	
		
		return Status.OK_STATUS;
	}
}
