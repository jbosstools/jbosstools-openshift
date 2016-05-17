package org.jboss.tools.openshift.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.jmx.core.providers.DefaultConnectionProvider;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

public class OpenShiftServerAdapter implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = new Class[]{IConnectionWrapper.class};

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IConnectionWrapper.class) {
			OpenShiftServer server= (OpenShiftServer) adaptableObject;
			String id = server.getServer().getId();
			DefaultConnectionProvider provider = (DefaultConnectionProvider) ExtensionManager.getProvider(DefaultConnectionProvider.PROVIDER_ID);
			IConnectionWrapper connection= provider.getConnection(id);
			if (connection == null) {
				try {
					connection = provider.createConnection(getJMXParameters(server));
				} catch (CoreException e) {
					OpenShiftCoreActivator.logError("could not create jmx connection", e);
				}
				provider.addConnection(connection);
			}
			return (T) connection;
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
	
	private Map<String, String> getJMXParameters(OpenShiftServer server) {
		Map<String, String> result= new HashMap<String, String>(4);
		result.put(DefaultConnectionProvider.ID, server.getServer().getId());
		result.put(DefaultConnectionProvider.USERNAME, "admin1234");
		result.put(DefaultConnectionProvider.PASSWORD, "Gurke123$");
		result.put(DefaultConnectionProvider.URL, "service:jmx:remote+http://localhost:9999");
		return result;
	}


}
