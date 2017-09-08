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
				ServiceManagerEnvironmentLoader type = ServiceManagerEnvironmentLoader.type(all[i]);
				if( !(type instanceof NullEnvironmentLoader)) {
					ServiceManagerEnvironment adb = type.getOrLoadServiceManagerEnvironment(all[i], true);
					IConnection con = util.findExistingOpenshiftConnection(all[i], adb);
					if( connection.equals(con)) {
						return all[i];
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getRegistryURL(IConnection connection) {
		IServer s = findServer(connection);
		if( s != null ) {
			ServiceManagerEnvironmentLoader type = ServiceManagerEnvironmentLoader.type(s);
			ServiceManagerEnvironment env = type.getOrLoadServiceManagerEnvironment(s, true);
			String dockerReg = env.getDockerRegistry();
			return dockerReg;
		}
		return null;
	}

}
