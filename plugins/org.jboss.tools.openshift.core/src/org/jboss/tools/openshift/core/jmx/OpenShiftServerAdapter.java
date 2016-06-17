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

import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionWrapper;
import org.jboss.tools.openshift.core.server.OpenShiftServer;

/**
 * Adapt OpenshiftServer to a IConnectionWrapper
 * @author Thomas Mäder
 *
 */
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
