package org.jboss.tools.openshift.core.connection.registry;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.connection.IConnection;

public interface IConnectionRegistryProvider {

	/**
	 * Get the registry url, or null if unable
	 * @param connection
	 * @return
	 */
	public IStatus getRegistryURL(IConnection connection);
}
