/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.util.ExpressResourceLabelUtils;
import org.jboss.tools.openshift.internal.common.core.job.FireConnectionsChangedJob;

import com.openshift.client.IApplication;
import com.openshift.client.IUser;

/**
 * @author Andre Dietisheim
 */
public class FireExpressConnectionsChangedJob extends FireConnectionsChangedJob {

	private LoadApplicationJob job;
	
	public FireExpressConnectionsChangedJob(LoadApplicationJob job) {
		super(NLS.bind("Refreshing connection for application {0}", job.getApplicationName()));
		this.job = job;
	}

	public FireExpressConnectionsChangedJob(IUser user) {
		super(NLS.bind("Refreshing connection {0}", user.getRhlogin()));
		add(createConnection(user));
	}

	public FireExpressConnectionsChangedJob(List<IUser> users) {
		super(NLS.bind("Refreshing {0} connections", users.size()));
		add(createConnections(users));
	}
	
	public FireExpressConnectionsChangedJob(ExpressConnection connection) {
		super(NLS.bind("Refreshing connection {0}", ExpressResourceLabelUtils.toString(connection)));
		add(connection);
	}

	@Override
	protected List<IConnection> getConnections() {
		if (!connections.isEmpty()) {
			return connections;
		} else if (job != null) {
			return Collections.singletonList(createConnection(job.getApplication()));
		}
		return Collections.emptyList();
	}

	private void add(Collection<IConnection> connections) {
		for (IConnection connection : connections) {
			add(connection);
		}
	}

	private void add(IConnection connection) {
		if (connection != null) {
			connections.add(connection);
		}
	}

	private Collection<IConnection> createConnections(List<IUser> users) {
		Set<IConnection> connections = new HashSet<IConnection>();
		if (users == null) {
			return connections;
		}
		
		for (IUser user : users) {
			connections.add(createConnection(user));
		}
		return connections;
	}

	private IConnection createConnection(IUser user) {
		return ExpressConnectionUtils.getByResource(user, ConnectionsRegistrySingleton.getInstance());
	}

	private IConnection createConnection(IApplication application) {
		if (application == null
				|| application.getDomain() == null) {
			return null;
		}
		return createConnection(application.getDomain().getUser());
	}

}
