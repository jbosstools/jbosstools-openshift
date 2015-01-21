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
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

/**
 * @author Andre Dietisheim
 */
public class RefreshConnectionJob extends AbstractDelegatingMonitorJob {

	private ExpressConnection connection;

	public RefreshConnectionJob(ExpressConnection connection) {
		super(NLS.bind("Refreshing iConnection {0}", connection.getId()));
		this.connection = connection;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		connection.refresh();
		return Status.OK_STATUS;
	}
}
