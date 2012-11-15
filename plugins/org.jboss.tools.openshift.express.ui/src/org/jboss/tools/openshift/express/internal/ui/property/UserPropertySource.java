/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.property;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionURL;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class UserPropertySource implements IPropertySource {

	private static final String PROPERTY_DOMAIN = "Domain";
	private static final String PROPERTY_USERNAME = "Username";
	/** the key that's used to store the connection (in the preferences) **/
	private static final String PROPERTY_KEY = "Key";

	private final Connection connection;

	public UserPropertySource(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new PropertyDescriptor(PROPERTY_KEY, PROPERTY_KEY),
				new PropertyDescriptor(PROPERTY_USERNAME, PROPERTY_USERNAME),
				new PropertyDescriptor(PROPERTY_DOMAIN, PROPERTY_DOMAIN) };
	}

	@Override
	public Object getPropertyValue(Object id) {
		try {
			if (!connection.isConnected() && !connection.canPromptForPassword()) {
				return OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL;
			}

			boolean requiresConnect = !connection.isConnected() && connection.canPromptForPassword();
			if (requiresConnect || !connection.isLoaded()) {
				loadRemoteDetails(connection);
			}

			if (PROPERTY_USERNAME.equals(id)) {
				return getUsername(connection);
			} else if (PROPERTY_DOMAIN.equals(id) && connection.hasDomain()) {
				return getDomain(connection);
			} else if (PROPERTY_KEY.equals(id)) {
				return getKey(connection);
			}
		} catch (OpenShiftException e) {
			Logger.error("Could not get selected object's property '" + id + "'.", e);
		}
		return null;
	}

	private Object getUsername(Connection connection) {
		return connection.getUsername();
	}

	private Object getDomain(Connection connection) {
		return connection.getDefaultDomain().getId()
				+ "." + connection.getDefaultDomain().getSuffix();
	}

	private String getKey(Connection connection) {
		try {
			return ConnectionURL.forConnection(connection).toString();
		} catch (UnsupportedEncodingException e) {
			// ignore
		} catch (MalformedURLException e) {
			// ignore
		}
		return "";
	}

	private void loadRemoteDetails(final Connection connection) throws OpenShiftException {
		IRunnableWithProgress longRunning = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				monitor.beginTask("Loading Connection Details", 200);
				try {
					if (!connection.isConnected()
							&& connection.canPromptForPassword()) {
						connection.connect();
					}
					monitor.worked(100);
					if (connection.isConnected())
						connection.load();
					monitor.worked(100);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(longRunning);
		} catch (InvocationTargetException ite) {
			Throwable cause = ite.getCause();
			if (cause instanceof OpenShiftException) {
				throw (OpenShiftException) cause;
			} else {
				new OpenShiftException(cause,
						"Could not load details for user {0} on {1}", connection.getUsername(), connection.getHost());
			}
		} catch (InterruptedException ie) {
		}
	}

	@Override
	public void resetPropertyValue(Object id) {
		// not implemented
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// not implemented
	}

	@Override
	public boolean isPropertySet(Object id) {
		return PROPERTY_USERNAME.equals(id)
				|| PROPERTY_DOMAIN.equals(id)
				|| PROPERTY_KEY.equals(id);
	}

}
