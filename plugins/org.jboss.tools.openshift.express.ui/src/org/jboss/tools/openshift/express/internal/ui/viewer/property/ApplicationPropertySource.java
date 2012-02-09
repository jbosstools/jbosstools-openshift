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

import java.text.SimpleDateFormat;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

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
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { new TextPropertyDescriptor("3.URL", "Public URL"),
				new TextPropertyDescriptor("1.Name", "Name"), 
				new TextPropertyDescriptor("6.UUID", "UUID"), 
				new TextPropertyDescriptor("5.Git URI", "Git URI"), 
				new TextPropertyDescriptor("2.Type", "Type"), 
				new TextPropertyDescriptor("4.Created on", "Created on") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		try {
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
				return application.getGitUri();
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
