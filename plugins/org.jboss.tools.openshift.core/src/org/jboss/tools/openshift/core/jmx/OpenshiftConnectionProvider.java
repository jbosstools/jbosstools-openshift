/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXConnectorProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListenerManager;
import org.jboss.tools.jmx.core.AbstractConnectionProvider;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

/**
 * ConnectionProvider implementation for EAP/Wildlfy on running on Openshift3 in
 * debug mode.
 * 
 * @author Thomas Mäder
 *
 */
public class OpenshiftConnectionProvider extends AbstractConnectionProvider {
	private class ServerListener extends UnitedServerListener {
		@Override
		public boolean canHandleServer(IServer server) {
			return true;
		}

		public void serverChanged(ServerEvent event) {
			updateConnection(event.getServer());
		}

		public void serverAdded(IServer server) {
			updateConnection(server);
		}

		public void serverChanged(IServer server) {
			updateConnection(server);
		}

		public void serverRemoved(IServer server) {
			updateConnection(server);
		}
	}

	public static final String ID = "com.jboss.tools.jmx.core.remoting";

	// also serves as mutex for adding/removing connections 
	private Map<String, IConnectionWrapper> idToConnection = new HashMap<String, IConnectionWrapper>();
	private Set<String> inCreation = new HashSet<>();

	public OpenshiftConnectionProvider() {
		UnitedServerListenerManager.getDefault().addListener(new ServerListener());
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName(IConnectionWrapper wrapper) {
		if (wrapper instanceof OpenshiftConnectionWrapper) {
			return ((OpenshiftConnectionWrapper) wrapper).getName();
		}
		return null;
	}

	private IConnectionWrapper createConnection(IServer server) throws MalformedURLException {
		ConnectorProviderRegistry connectorProviders = OpenShiftCoreActivator.getDefault().getConnectorProviders();
		VersionKey version = new VersionDetector(server).guess();
		if (version != null) {
			JMXConnectorProvider provider = connectorProviders.getProvider(version);
			if (provider != null) {
				return new OpenshiftConnectionWrapper(this, server, provider);
			}
		}
		return null;
	}

	@Override
	public IConnectionWrapper[] getConnections() {
		synchronized (idToConnection) {
			return idToConnection.values().toArray(new IConnectionWrapper[idToConnection.values().size()]);
		}
	}

	@Override
	public boolean canCreate() {
		return false;
	}

	@Override
	public boolean canDelete(IConnectionWrapper wrapper) {
		return false;
	}

	@Override
	public IConnectionWrapper createConnection(@SuppressWarnings("rawtypes") Map map) throws CoreException {
		throw new UnsupportedOperationException("Cannot create connection");
	}

	@Override
	public void addConnection(IConnectionWrapper connection) {
		throw new UnsupportedOperationException("Cannot add connection");
	}

	@Override
	public void removeConnection(IConnectionWrapper connection) {
		throw new UnsupportedOperationException("Cannot remove connection");
	}

	@Override
	public void connectionChanged(IConnectionWrapper connection) {
		// ignore
	}

	private boolean shouldHaveConnection(IServer server) {
		return ILaunchManager.DEBUG_MODE.equals(server.getLaunch().getLaunchMode())
				&& server.getServerState() == IServer.STATE_STARTED && isOpenshiftServer(server);
	}

	private boolean isOpenshiftServer(IServer server) {
		return server.getAdapter(OpenShiftServer.class) != null;
	}

	public IConnectionWrapper getConnection(String id2) {
		synchronized (idToConnection) {
			return idToConnection.get(id2);
		}
	}

	private void updateConnection(IServer server) {
		boolean shouldHaveConnection = shouldHaveConnection(server);
		IConnectionWrapper removed = null;
		synchronized (idToConnection) {
			IConnectionWrapper con = idToConnection.get(server.getId());
			if (con == null && shouldHaveConnection && !inCreation.contains(server.getId())) {
				inCreation.add(server.getId());
				startConnectionCreateJob(server);
			} else if (con != null && !shouldHaveConnection) {
				try {
					con.disconnect();
				} catch (IOException e) {
					OpenShiftCoreActivator.logWarning("Could not disconnect jmx connection", e);
				}
				idToConnection.remove(server.getId());
				removed = con;
			}
		}

		if (removed != null) {
			fireRemoved(removed);
		} 
	}

	/*
	 * Creates a connection in a background job. Call under mutex.
	 */
	private void startConnectionCreateJob(IServer server) {
		new Job("Create Connection") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IConnectionWrapper added = createConnection(server);
					synchronized (idToConnection) {
						idToConnection.put(server.getId(), added);
					}
					fireAdded(added);
					return Status.OK_STATUS;
				} catch (MalformedURLException e) {
					return new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, "Could not create jmx connection", e);
				} finally {
					synchronized (idToConnection) {
						inCreation.remove(server.getId());
					}
				}
			}
		}.schedule();
	}

}
