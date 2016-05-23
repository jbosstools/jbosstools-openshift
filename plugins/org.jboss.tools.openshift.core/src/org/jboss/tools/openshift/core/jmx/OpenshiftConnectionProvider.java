package org.jboss.tools.openshift.core.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListenerManager;
import org.jboss.tools.jmx.core.AbstractConnectionProvider;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

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

	private Map<String, IConnectionWrapper> idToConnection = new HashMap<String, IConnectionWrapper>();

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
			return new OpenshiftConnectionWrapper(this, server, provider);
		}
		return null;
	}

	@Override
	public IConnectionWrapper[] getConnections() {
		return idToConnection.values().toArray(new IConnectionWrapper[idToConnection.values().size()]);
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
		return idToConnection.get(id2);
	}

	private void updateConnection(IServer server) {
		IConnectionWrapper con = idToConnection.get(server.getId());
		boolean shouldHaveConnection = shouldHaveConnection(server);
		if (con == null && shouldHaveConnection) {
			try {
				con = createConnection(server);
				idToConnection.put(server.getId(), con);
				fireAdded(con);
			} catch (MalformedURLException e) {
				OpenShiftCoreActivator.logError("Could not create jmx connection", e);
			}
		} else if (con != null && !shouldHaveConnection) {
			try {
				con.disconnect();
			} catch (IOException e) {
				OpenShiftCoreActivator.logWarning("Could not disconnect jmx connection", e);
			}
			idToConnection.remove(server.getId());
			fireRemoved(con);
		}
	}

}
