/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;

public class ConnectionPropertySource implements IPropertySource {
	
	private static final String HOST = "host";
	private static final String USERNAME = "username";
	private IConnection connection;

	private ConnectionListener listener = new ConnectionListener();

	class ConnectionListener implements IConnectionsRegistryListener {

		@Override
		public void connectionAdded(IConnection connection) {
		}

		@Override
		public void connectionRemoved(IConnection connection) {
		}

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if(connection.equals(ConnectionPropertySource.this.connection) && IOpenShiftConnection.PROPERTY_EXTENDED_PROPERTIES.equals(property)) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						PropertySheet sh = OpenShiftUIUtils.getPropertySheet();
						if(sh != null) {
							OpenShiftUIUtils.refreshPropertySheetPage(sh);
						}
					}
				});
			}
		}
	}

	public ConnectionPropertySource(IConnection connection) {
		this.connection = connection;
		ConnectionsRegistrySingleton.getInstance().addListener(listener);
	}

	@Override
	public Object getEditableValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor>  descriptors = new ArrayList<>();
		descriptors.add(new UneditablePropertyDescriptor(HOST, "Host"));
		descriptors.add(new UneditablePropertyDescriptor(USERNAME, "User Name"));
		if(connection instanceof IOpenShiftConnection) {
			Set<String> set = new TreeSet<>(((IOpenShiftConnection)connection).getExtendedProperties().keySet());
			for (String name: set) {
				descriptors.add(new UneditablePropertyDescriptor(name, toVisualPropertyName(name)));
			}
		}
		return descriptors.toArray(new IPropertyDescriptor[descriptors.size()]);
	}

	private String toVisualPropertyName(String name) {
		String label = ICommonAttributes.EXTENDED_PROPERTY_LABELS.get(name);
		if(label != null) {
			return label;
		}
		if(name.length() > 1) {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		}
		return name;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(HOST.equals(id)){
			return connection.toString();
		}
		if(USERNAME.equals(id))
			return connection.getUsername();
		if(connection instanceof IOpenShiftConnection && id != null) {
			Object result = ((IOpenShiftConnection)connection).getExtendedProperties().get(id);
			return result == null ? "" : result.toString();
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id	, Object value) {
		
	}

	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(listener);
	}
}
