/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;

public class EnvironmentVariable implements IKeyValueItem {

	private String name;
	private String value;
	private boolean isNew;

	/**
	 * 
	 * @param name
	 * @param value
	 * @param isNew   true if the value is new (e.g. not declared in a docker image
	 */
	public EnvironmentVariable(String name, String value, boolean isNew){
		this.name = name;
		this.value = value;
		this.isNew = isNew;
	}
	
	public EnvironmentVariable(String name, String value){
		this(name,value, false);
	}
	
	@Override
	public String getKey() {
		return name;
	}

	@Override
	public void setKey(String key) {
		this.name = key;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * 
	 * @return true if this is a new value
	 */
	public boolean isNew() {
		return isNew;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isNew ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		EnvironmentVariable other = (EnvironmentVariable) obj;
		if (isNew != other.isNew)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	

}
