package org.jboss.tools.openshift.cdk.server.core.internal.registry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironmentLoader;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironmentLoader.NullEnvironmentLoader;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.registry.IConnectionRegistryProvider;

public class CDKRegistryProvider implements IConnectionRegistryProvider {
	private class Pair {
		private MultiStatus ms;
		private IServer s;

		public Pair(MultiStatus ms, IServer s) {
			this.ms = ms;
			this.s = s;
		}
	}

	private Pair findServer(IConnection connection) {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		IServer[] all = ServerCore.getServers();
		MultiStatus ms = new MultiStatus(CDKCoreActivator.PLUGIN_ID, 0,
				"CDKRegistryProvider unable to locate a registry URL for openshift connection " + connection.toString(),
				null);
		if (all.length == 0) {
			ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, "No Servers exist in the workspace."));
		}

		boolean foundMinishift = false;
		for (int i = 0; i < all.length; i++) {
			ServiceManagerEnvironment env = getServiceManagerEnvironment(all[i]);
			ServiceManagerEnvironmentLoader type = ServiceManagerEnvironmentLoader.type(all[i]);
			if (type.getType() != ServiceManagerEnvironmentLoader.TYPE_MINISHIFT) {
				continue;
			}
			foundMinishift = true;
			if (all[i].getServerState() != IServer.STATE_STARTED) {
				ms.add(new Status(IStatus.INFO, CDKCoreActivator.PLUGIN_ID,
						"Unable to locate the CDK environment details for stopped server " + all[i].getName()));
				continue;
			}
			if (env == null) {
				ms.add(new Status(IStatus.INFO, CDKCoreActivator.PLUGIN_ID,
						"Unable to locate the CDK environment details for server " + all[i].getName()));
				continue;
			}

			if (!util.serverMatchesConnection(all[i], connection, env)) {
				ms.add(util.serverConnectionMatchError(all[i], connection, env));
				continue;
			}

			String registry = env.getDockerRegistry();
			if (registry == null || registry.isEmpty()) {
				String s = "Server {0} does not have a registry associated with it. Please verify that 'minishift openshift registry' returns a valid URL.";
				ms.add(new Status(IStatus.INFO, CDKCoreActivator.PLUGIN_ID, NLS.bind(s, all[i].getName())));
				continue;
			}
			return new Pair(null, all[i]);
		}
		if (all.length > 0 && !foundMinishift) {
			ms.add(new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID,
					"No valid CDK Servers exist in the workspace."));
		}
		return new Pair(ms, null);
	}

	/*
	 * Test classes should override 
	 */
	protected ServiceManagerEnvironment getServiceManagerEnvironment(IServer server) {
		ServiceManagerEnvironmentLoader type = ServiceManagerEnvironmentLoader.type(server);
		if (!(type instanceof NullEnvironmentLoader)) {
			ServiceManagerEnvironment adb = type.getOrLoadServiceManagerEnvironment(server, true);
			return adb;
		}
		return null;
	}

	@Override
	public IStatus getRegistryURL(IConnection connection) {
		Pair s = findServer(connection);
		if (s != null && s.s != null) {
			ServiceManagerEnvironment env = getServiceManagerEnvironment(s.s);
			return new Status(IStatus.OK, CDKCoreActivator.PLUGIN_ID, env.getDockerRegistry());
		}
		return s.ms;
	}

}
