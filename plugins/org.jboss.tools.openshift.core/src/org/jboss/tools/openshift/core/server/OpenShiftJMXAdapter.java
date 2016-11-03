/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.core.server;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.jmx.core.ExtensionManager;
import org.jboss.tools.jmx.core.IConnectionWrapper;

public class OpenShiftJMXAdapter implements IAdapterFactory {
	private static final Class<?>[] ADAPTERS = new Class[]{IConnectionWrapper.class};

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IConnectionWrapper.class) {
			OpenShiftServer server= (OpenShiftServer) adaptableObject;
			OpenshiftJMXConnectionProvider provider = (OpenshiftJMXConnectionProvider) ExtensionManager.getProvider(OpenshiftJMXConnectionProvider.PROVIDER_ID);
			return (T) provider.findConnection(server.getServer());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
}

