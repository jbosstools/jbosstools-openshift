/*******************************************************************************
 * Copyright (c) 2013-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.core.job;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * @author Andre Dietisheim
 */
public class FireConnectionsChangedJob extends AbstractDelegatingMonitorJob {

	protected List<IConnection> connections = new ArrayList<>();
	
	public FireConnectionsChangedJob(IConnection connection) {
		super(NLS.bind("Refreshing connection {0}", connection.getHost()));
		add(connection);
	}

	protected FireConnectionsChangedJob(String jobName) {
		super(jobName);
	}
	
	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		for (IConnection connection : getConnections()) {
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection);
		}
		return Status.OK_STATUS;
	}
	
	protected List<IConnection> getConnections() {
		return connections;
	}

	private void add(IConnection connection) {
		if (connection != null) {
			connections.add(connection);
		}
	}
}
