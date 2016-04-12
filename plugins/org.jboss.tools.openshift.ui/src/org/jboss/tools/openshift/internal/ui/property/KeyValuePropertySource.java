package org.jboss.tools.openshift.internal.ui.property;

/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

public class KeyValuePropertySource implements IPropertySource {

	private Map<String, String> map = new HashMap<>();

	public KeyValuePropertySource(Map<String, String> variables){
		this.map = variables;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeyValuePropertySource other = (KeyValuePropertySource) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}
	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> desc = new ArrayList<>(map.size());
		for (Map.Entry<String, String> entry : map.entrySet()) {
			desc.add(new UneditablePropertyDescriptor(entry.getKey(), entry.getKey()));
		}
		return desc.toArray(new IPropertyDescriptor[]{});
	}

	@Override
	public Object getPropertyValue(Object id) {
		return map.get(id);
	}

	@Override
	public boolean isPropertySet(Object arg0) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object arg0) {
	}

	@Override
	public void setPropertyValue(Object arg0, Object arg1) {
	}

}
