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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

import com.openshift.client.IUser;

/**
 * @author Andre Dietisheim
 */
public class RefreshConnectionsModelJob extends AbstractDelegatingMonitorJob {

	private List<Connection> connections = new ArrayList<Connection>();

	public RefreshConnectionsModelJob(IUser user) {
		super(NLS.bind("Refreshing connection {0}", user.getRhlogin()));
		Connection connection = getConnection(user);
		add(connection);
	}

	public RefreshConnectionsModelJob(List<IUser> users) {
		super(NLS.bind("Refreshing {0} connections", users.size()));
		add(getConnections(users));
	}

	public RefreshConnectionsModelJob(Connection connection) {
		super(NLS.bind("Refreshing connection {0}", connection.getUsername()));
		add(connection);
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		for (Connection connection : connections) {
			ConnectionsModelSingleton.getInstance().fireConnectionChanged(connection);
		}
		return Status.OK_STATUS;
	}
	
	private void add(List<Connection> connections) {
		for (Connection connection : connections) {
			add(connection);
		}
	}

	private void add(Connection connection) {
		if (connection != null) {
			connections.add(connection);
		}
	}

	private List<Connection> getConnections(List<IUser> users) {
		List<Connection> connections = new ArrayList<Connection>();
		if (users == null) {
			return connections;
		}
		
		for (IUser user : users) {
			connections.add(getConnection(user));
		}
		return connections;
	}

	private Connection getConnection(IUser user) {
		Connection connection = ConnectionsModelSingleton.getInstance().getConnectionByResource(user);
		return connection;
	}
}
