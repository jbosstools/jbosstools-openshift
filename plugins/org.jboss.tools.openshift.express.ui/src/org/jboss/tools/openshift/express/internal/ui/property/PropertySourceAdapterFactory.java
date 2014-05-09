/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.property;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Xavier Coulon
 * 
 */
public class PropertySourceAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if (adapterType == IPropertySource.class) {
			if (adaptableObject instanceof Connection) {
				return new ConnectionPropertySource((Connection) adaptableObject);
			} else if (adaptableObject instanceof IDomain) {
				return new DomainPropertySource((IDomain) adaptableObject);
			} else if (adaptableObject instanceof IApplication) {
				return new ApplicationPropertySource((IApplication) adaptableObject);
			} else if (adaptableObject instanceof IEmbeddedCartridge) {
				return new EmbeddedCartridgePropertySource((IEmbeddedCartridge) adaptableObject);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
