/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftTimeoutException;

/**
 * @author Andre Dietisheim
 */
public class RestartApplicationJob extends AbstratApplicationJob {

	public RestartApplicationJob(IApplication application) {
		super(new ApplicationProvider(application), NLS.bind(ExpressUIMessages.RESTARTING_APPLICATION, application.getName()));
	}

	public RestartApplicationJob(LoadApplicationJob applicationJob) {
		super(new ApplicationProvider(applicationJob), NLS.bind(ExpressUIMessages.RESTARTING_APPLICATION, ""));
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			super.doRun(monitor);
		} catch(OpenShiftTimeoutException e) {
			// intentionally swallow, 
			// restart operation is very very likely to timeout
		}
		return Status.OK_STATUS;
	}

	@Override
	protected void doRun(IApplication application) {
		application.restart();
	}
}
