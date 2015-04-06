/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * A UI class to facilitate grouping like resources
 * in the explorer view
 */
public class ResourceGrouping{
	private IProject project;
	private ResourceKind kind;

	/**
	 * 
	 * @param title            The title to display
	 * @param resources   The resource of this grouping
	 */
	public ResourceGrouping(ResourceKind kind, IProject project) {
		this.kind = kind;
		this.project = project;
	}

	public ResourceKind getKind() {
		return this.kind;
	}

	public Object[] getResources() {
		return project.getResources(kind).toArray();
	}

	@Override
	public String toString() {
		return kind.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		return result;
	}

	/**
	 * Resource Groupings are considered equal if they have
	 * the same title.  The contained resources is not
	 * considered
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceGrouping other = (ResourceGrouping) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		return true;
	}

}