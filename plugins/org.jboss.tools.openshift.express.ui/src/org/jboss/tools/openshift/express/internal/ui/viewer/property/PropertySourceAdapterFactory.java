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
package org.jboss.tools.openshift.express.internal.ui.viewer.property;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IEmbeddableCartridge;

/**
 * @author Xavier Coulon
 * 
 */
public class PropertySourceAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if(adapterType == IPropertySource.class) {
			if(adaptableObject instanceof UserDelegate) {
				return new UserPropertySource((UserDelegate)adaptableObject);
			}
			if(adaptableObject instanceof IApplication) {
				return new ApplicationPropertySource((IApplication)adaptableObject);
			}if(adaptableObject instanceof IEmbeddableCartridge) {
				return new EmbeddableCartridgePropertySource((IEmbeddableCartridge)adaptableObject);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
