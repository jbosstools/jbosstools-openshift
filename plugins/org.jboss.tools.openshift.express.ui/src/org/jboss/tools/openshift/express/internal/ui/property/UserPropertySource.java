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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class UserPropertySource implements IPropertySource {

	private static final String PROPERTY_DOMAIN = "Domain";
	private static final String PROPERTY_USERNAME = "Username";
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
				new PropertyDescriptor(PROPERTY_USERNAME, PROPERTY_USERNAME),
				new PropertyDescriptor(PROPERTY_DOMAIN, PROPERTY_DOMAIN) };
	}

	@Override
	public Object getPropertyValue(Object id) {
		try {
			if(!connection.isConnected() && !connection.canPromptForPassword()) {
				return OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL;
			}
				
			boolean requiresConnect = !connection.isConnected() && connection.canPromptForPassword();
			if( requiresConnect || !connection.isLoaded()) {
				loadRemoteDetails();
			}
			
			if (id.equals(PROPERTY_USERNAME)) {
				return connection.getUsername();
			}
			if (id.equals(PROPERTY_DOMAIN) && connection.hasDomain()) {
				return connection.getDefaultDomain().getId() + "." + connection.getDefaultDomain().getSuffix();
			}
		} catch (OpenShiftException e) {
		 	Logger.error("Could not get selected object's property '" + id + "'.", e);
		}
		return null;
	}

	private void loadRemoteDetails() throws OpenShiftException  {
		IRunnableWithProgress longRunning = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException  {
				monitor.beginTask("Checking Remote Details", 200);
				try {
					if( !connection.isConnected() 
							&& connection.canPromptForPassword()) {
						connection.connect();
					}
					monitor.worked(100);
					if( connection.isConnected())
						connection.load();
					monitor.worked(100);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(longRunning);
		} catch( InvocationTargetException ite ) {
			Throwable t = ite.getCause();
			throw (OpenShiftException)t;
		} catch( InterruptedException ie ) {
		}
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
