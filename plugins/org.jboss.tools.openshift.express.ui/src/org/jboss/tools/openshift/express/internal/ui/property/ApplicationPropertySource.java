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

import java.text.SimpleDateFormat;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 */
public class ApplicationPropertySource implements IPropertySource {

	private final IApplication application;

	public ApplicationPropertySource(IApplication adaptableObject) {
		this.application = adaptableObject;
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { new PropertyDescriptor("3.URL", "Public URL"),
				new PropertyDescriptor("1.Name", "Name"), 
				new PropertyDescriptor("6.UUID", "UUID"), 
				new PropertyDescriptor("5.Git URI", "Git URI"), 
				new PropertyDescriptor("2.Type", "Type"), 
				new PropertyDescriptor("4.Created on", "Created on"), new PropertyDescriptor("7.Port Forwarding", "Port Forwarding") }; 
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id.equals("3.URL")) {
			return application.getApplicationUrl().toString();
		}
		if (id.equals("1.Name")) {
			return application.getName();
		}
		if (id.equals("6.UUID")) {
			return application.getUUID();
		}
		if (id.equals("4.Created on")) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
			return format.format(application.getCreationTime());
		}

		if (id.equals("2.Type")) {
			return application.getCartridge().getName();
		}
		if (id.equals("5.Git URI")) {
			return application.getGitUrl();
		}
		if(id.equals("7.Port Forwarding")) {
			try {
				StringBuffer bf = new StringBuffer();
				boolean portFowardingStarted = application.isPortFowardingStarted();
				
				if (portFowardingStarted == true) {
					return bf.append("Yes");
				} else if (portFowardingStarted == false) {
					return	bf.append("No");
				}
				
			} catch (OpenShiftSSHOperationException e) {
				return "Unknown"; //e.printStackTrace();
			}
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
