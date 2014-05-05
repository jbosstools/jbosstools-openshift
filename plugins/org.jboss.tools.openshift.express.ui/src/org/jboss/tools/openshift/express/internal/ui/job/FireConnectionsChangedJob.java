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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;

/**
 * @author Andre Dietisheim
 */
public class FireConnectionsChangedJob extends AbstractDelegatingMonitorJob {

	private List<Connection> connections = new ArrayList<Connection>();
	private LoadApplicationJob job;
	
	public FireConnectionsChangedJob(LoadApplicationJob job) {
		super("Refreshing connections");
		this.job = job;
	}

	public FireConnectionsChangedJob(IUser user) {
		super(NLS.bind("Refreshing connection {0}", user.getRhlogin()));
		add(createConnection(user));
	}

	public FireConnectionsChangedJob(List<IUser> users) {
		super(NLS.bind("Refreshing {0} connections", users.size()));
		add(createConnections(users));
	}

	public FireConnectionsChangedJob(Connection connection) {
		super(NLS.bind("Refreshing connection {0}", connection.getUsername()));
		add(connection);
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		for (Connection connection : getConnections()) {
			ConnectionsModelSingleton.getInstance().fireConnectionChanged(connection);
		}
		return Status.OK_STATUS;
	}
	
	private List<Connection> getConnections() {
		if (!connections.isEmpty()) {
			return connections;
		} else if (job != null) {
			return Collections.singletonList(createConnection(job.getApplication()));
		}
		return Collections.emptyList();
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

	private List<Connection> createConnections(List<IUser> users) {
		List<Connection> connections = new ArrayList<Connection>();
		if (users == null) {
			return connections;
		}
		
		for (IUser user : users) {
			connections.add(createConnection(user));
		}
		return connections;
	}

	private Connection createConnection(IUser user) {
		Connection connection = ConnectionsModelSingleton.getInstance().getConnectionByResource(user);
		return connection;
	}

	private Connection createConnection(IApplication application) {
		if (application == null
				|| application.getDomain() == null) {
			return null;
		}
		return createConnection(application.getDomain().getUser());
	}

}
