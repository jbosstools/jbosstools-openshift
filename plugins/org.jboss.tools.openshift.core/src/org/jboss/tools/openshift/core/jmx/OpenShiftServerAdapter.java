package org.jboss.tools.openshift.core.jmx;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.openshift.core.server.OpenShiftServer;

public class OpenShiftServerAdapter implements IAdapterFactory {
	private static final Class<?>[] ADAPTERS = new Class[]{IConnectionWrapper.class};

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IConnectionWrapper.class) {
			OpenShiftServer server= (OpenShiftServer) adaptableObject;
			OpenshiftConnectionProvider provider = (OpenshiftConnectionProvider) ExtensionManager.getProvider(OpenshiftConnectionProvider.ID);
			return (T) provider.getConnection(server.getServer().getId());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
	

}
