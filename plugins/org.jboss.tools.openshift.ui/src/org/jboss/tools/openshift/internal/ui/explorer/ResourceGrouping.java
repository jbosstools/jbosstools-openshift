/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import org.jboss.tools.openshift.common.core.IRefreshable;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;

/**
 * A UI class to facilitate grouping like resources
 * in the explorer view
 */
public class ResourceGrouping implements IRefreshable {
	private IProject project;
	private ResourceKind kind;
	private IRefreshable refreshable;

	/**
	 * 
	 * @param title            The title to display
	 * @param resources   The resource of this grouping
	 */
	public ResourceGrouping(ResourceKind kind, IProject project) {
		this.kind = kind;
		this.project = project;
	}
	public void setRefreshable(IRefreshable refreshable) {
		this.refreshable = refreshable;
	}
	
	public IProject getProject() {
		return this.project;
	}
	
	public ResourceKind getKind() {
		return this.kind;
	}

	@Override
	public String toString() {
		return kind.toString();
	}

	@Override
	public void refresh() {
		if(refreshable == this || refreshable == null) return;
		this.refreshable.refresh();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ResourceGrouping))
			return false;
		ResourceGrouping other = (ResourceGrouping) obj;
		if (kind != other.kind)
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		return true;
	}
}