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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class EmbeddableCartridgePropertySource implements IPropertySource {

	private final IEmbeddableCartridge cartridge;

	public EmbeddableCartridgePropertySource(IEmbeddableCartridge cartridge) {
		this.cartridge = cartridge;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { new TextPropertyDescriptor("Name", "Name"),
				new TextPropertyDescriptor("URL", "URL") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		try {
			if (id.equals("Name")) {
				return cartridge.getName();
			}
			if (id.equals("URL")) {
				return cartridge.getUrl();
			}
		} catch (OpenShiftException e) {
			Logger.error("Could not get selected object's property '" + id + "'.", e);
		}
		return null;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

}
