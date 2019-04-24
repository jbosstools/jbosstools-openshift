/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheet;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;

public class ConnectionPropertySource implements IPropertySource {

	private static final String HOST = "host";
	private static final String USERNAME = "username";
	private static final String OPENSHIFT_MASTER_VERSION = "openshift-version";
	private static final String KUBERNETES_MASTER_VERSION = "kubernetes-version";
	private static final String OC_CLIENT = "oc-client";
	private IConnection connection;

	private ConnectionListener listener = new ConnectionListener();

	class ConnectionListener extends ConnectionsRegistryAdapter {

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (connection.equals(ConnectionPropertySource.this.connection)
					&& IOpenShiftConnection.PROPERTY_EXTENDED_PROPERTIES.equals(property)) {
				Display.getDefault().asyncExec(() -> {
						PropertySheet sh = OpenShiftUIUtils.getPropertySheet();
						if (sh != null) {
							OpenShiftUIUtils.refreshPropertySheetPage(sh);
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
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> descriptors = new ArrayList<>();
		descriptors.add(new UneditablePropertyDescriptor(HOST, "Host"));
		descriptors.add(new UneditablePropertyDescriptor(USERNAME, "User Name"));
		descriptors.add(new UneditablePropertyDescriptor(OPENSHIFT_MASTER_VERSION, "OpenShift Master Version"));
		descriptors.add(new UneditablePropertyDescriptor(KUBERNETES_MASTER_VERSION, "Kubernetes Master Version"));
		descriptors.add(new UneditablePropertyDescriptor(OC_CLIENT, "OC Client"));
		if (connection instanceof IOpenShiftConnection) {
			Set<String> set = new TreeSet<>(((IOpenShiftConnection) connection).getExtendedProperties().keySet());
			for (String name : set) {
				String label = toVisualPropertyName(name);
				if (StringUtils.isNotBlank(label)) {
					descriptors.add(new UneditablePropertyDescriptor(name, label));
				}
			}
		}
		return descriptors.toArray(new IPropertyDescriptor[descriptors.size()]);
	}

	private String toVisualPropertyName(String name) {
		String label = ICommonAttributes.EXTENDED_PROPERTY_LABELS.get(name);
		if (label != null) {
			return label;
		}
		if (name.length() > 1) {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		}
		return name;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id == null) {
			return null;
		}
		if (HOST.equals(id)) {
			return this.connection.toString();
		}
		if (USERNAME.equals(id)) {
			return this.connection.getUsername();
		}
		if (connection instanceof IOpenShiftConnection) {
			IOpenShiftConnection openshiftConnection = (IOpenShiftConnection) this.connection;
			if (OPENSHIFT_MASTER_VERSION.equals(id)) {
				return openshiftConnection.getOpenShiftMasterVersion();
			}
			if (KUBERNETES_MASTER_VERSION.equals(id)) {
				return openshiftConnection.getKubernetesMasterVersion();
			}
			if (OC_CLIENT.equals(id)) {
				return OCBinary.getInstance().getPath(openshiftConnection);
			}
			Object result = openshiftConnection.getExtendedProperties().get(id);
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
		// nothing to do
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// nothing to do
	}

	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(listener);
	}
}
