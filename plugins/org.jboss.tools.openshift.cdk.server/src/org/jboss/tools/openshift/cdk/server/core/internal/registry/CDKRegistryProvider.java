package org.jboss.tools.openshift.cdk.server.core.internal.registry;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironmentLoader;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironmentLoader.NullEnvironmentLoader;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.registry.IConnectionRegistryProvider;

public class CDKRegistryProvider implements IConnectionRegistryProvider {

	private IServer findServer(IConnection connection) {
		CDKOpenshiftUtility util = new CDKOpenshiftUtility();
		IServer[] all = ServerCore.getServers();
		for( int i = 0; i < all.length; i++ ) {
			if( all[i].getServerState() == IServer.STATE_STARTED ) {
				ServiceManagerEnvironment env = getServiceManagerEnvironment(all[i]);
				if( env != null ) {
					IConnection con = util.findExistingOpenshiftConnection(all[i], env);
					if( connection.equals(con)) {
						return all[i];
					}
				}
			}
		}
		return null;
	}
	
	/*
	 * Test classes should override 
	 */
	protected ServiceManagerEnvironment getServiceManagerEnvironment(IServer server) {
		ServiceManagerEnvironmentLoader type = ServiceManagerEnvironmentLoader.type(server);
		if( !(type instanceof NullEnvironmentLoader)) {
			ServiceManagerEnvironment adb = type.getOrLoadServiceManagerEnvironment(server, true);
			return adb;
		}
		return null;
	}

	@Override
	public String getRegistryURL(IConnection connection) {
		IServer s = findServer(connection);
		if( s != null ) {
			ServiceManagerEnvironment env = getServiceManagerEnvironment(s);
			return env.getDockerRegistry();
		}
		return null;
	}

}
