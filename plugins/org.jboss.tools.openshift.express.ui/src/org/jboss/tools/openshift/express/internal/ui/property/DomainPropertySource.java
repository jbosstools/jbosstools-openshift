/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceLabelUtils;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class DomainPropertySource implements IPropertySource {

	private static final String PROPERTY_ID = "ID";
	private static final String PROPERTY_SUFFIX = "SUFFIX";
	private static final String PROPERTY_FULLNAME = "FULLNAME";
	
	private final IDomain domain;

	public DomainPropertySource(IDomain domain) {
		this.domain = domain;
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] { 
				new PropertyDescriptor(PROPERTY_ID, "Id"),
				new PropertyDescriptor(PROPERTY_SUFFIX, "Suffix"),
				new PropertyDescriptor(PROPERTY_FULLNAME, "Full Name") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (PROPERTY_ID.equals(id)) {
			return domain.getId();
		} else if (PROPERTY_SUFFIX.equals(id)) {
			return domain.getSuffix();
		} else if (PROPERTY_FULLNAME.equals(id)) {
			return OpenShiftResourceLabelUtils.toString(domain);
		}
		return null;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

}
