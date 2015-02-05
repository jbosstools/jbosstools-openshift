/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Andre Dietisheim
 * 
 */
public class ExpressConnectionAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if (adapterType != ExpressConnection.class) {
			return null;
		}

		ExpressConnection connection = null;
		if (adaptableObject instanceof ExpressConnection) {
			connection = (ExpressConnection) adaptableObject;
		} else if (adaptableObject instanceof IDomain) {
			IDomain domain = (IDomain) adaptableObject;
			connection = ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance());
		} else if (adaptableObject instanceof IUser) {
			IUser user = (IUser) adaptableObject;
			connection = ExpressConnectionUtils.getByResource(user, ConnectionsRegistrySingleton.getInstance());
		} else if (adaptableObject instanceof IApplication) {
			IApplication application = (IApplication) adaptableObject;
			connection = getConnection(application);
		} else if (adaptableObject instanceof IEmbeddedCartridge) {
			IEmbeddedCartridge embeddedCartridge = (IEmbeddedCartridge) adaptableObject;
			IApplication application = embeddedCartridge.getApplication();
			if (application != null) {				
				connection = getConnection(application);
			}
		}
		return connection;
	}

	private ExpressConnection getConnection(IApplication application) {
		IDomain domain = application.getDomain();
		if (domain == null) {
			return null;
		}
		return ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance());
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ExpressConnection.class };
	}

}
