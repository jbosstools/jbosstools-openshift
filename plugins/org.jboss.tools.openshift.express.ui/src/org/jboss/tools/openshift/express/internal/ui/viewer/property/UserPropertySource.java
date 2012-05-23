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

import java.net.SocketTimeoutException;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class UserPropertySource implements IPropertySource {

	private static final String PROPERTY_DOMAIN = "Domain";
	private static final String PROPERTY_USERNAME = "Username";
	private final UserDelegate user;

	public UserPropertySource(UserDelegate user) {
		this.user = user;
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
			if(!user.isConnected() && !user.canPromptForPassword()) {
				return OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL;
			}
			if(!user.isConnected() && user.canPromptForPassword()) {
				user.checkForPassword();
			}
			
			if (id.equals(PROPERTY_USERNAME)) {
				return user.getUsername();
			}
			if (id.equals(PROPERTY_DOMAIN) && user.hasDomain()) {
				return user.getDefaultDomain().getId() + "." + user.getDefaultDomain().getSuffix();
			}
		} catch (OpenShiftException e) {
		 	Logger.error("Could not get selected object's property '" + id + "'.", e);
		} catch (SocketTimeoutException e) {
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
