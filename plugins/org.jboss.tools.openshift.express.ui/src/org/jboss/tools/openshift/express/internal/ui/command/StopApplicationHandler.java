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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.job.LoadApplicationJob;
import org.jboss.tools.openshift.express.internal.ui.job.StopApplicationJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 */
public class StopApplicationHandler extends AbstractApplicationHandler {

	@Override
	protected IStatus execute(IApplication application, Shell shell) {
		new StopApplicationJob(application).schedule();
		return Status.OK_STATUS;
	}

	@Override
	protected IStatus execute(LoadApplicationJob job, Shell shell) {
		new JobChainBuilder(job)
				.runWhenSuccessfullyDone(new StopApplicationJob(job)).schedule();
		return Status.OK_STATUS;
	}

	@Override
	protected String getOperationName() {
		return "stop application";
	}
}
