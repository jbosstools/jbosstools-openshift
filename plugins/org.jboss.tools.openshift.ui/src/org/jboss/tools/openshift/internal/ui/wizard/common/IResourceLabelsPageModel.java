/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;

/**
 * @author jeff.cantrill
 */
public interface IResourceLabelsPageModel {
	String PROPERTY_SELECTED_LABEL = "selectedLabel";
	String PROPERTY_LABELS = "labels";
	
	List<Label> getLabels();
	
	void setLabels(List<Label> labels);
	
	Collection<String> getReadOnlyLabels();
	
	void setSelectedLabel(Label label);
	
	Label getSelectedLabel();
	
	void removeLabel(Label label);
	
	void updateLabel(Label label, String key, String value);

	void addLabel(String key, String value);
	
	/**
	 * Adapter to labels on an IResource
	 * @author jeff.cantrill
	 *
	 */
	static final class Label implements IKeyValueItem{
		private String name;
		private String value;

		public Label(String name, String value){
			this.name = name;
			this.value = value;
		}

		@Override
		public void setKey(String key) {
			setName(key);
		}

		public String getName() {
			return this.name;
		}
		
		@Override
		public String getKey() {
			return getName();
		}

		@Override
		public String getValue() {
			return value;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			Label other = (Label) obj;
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


}