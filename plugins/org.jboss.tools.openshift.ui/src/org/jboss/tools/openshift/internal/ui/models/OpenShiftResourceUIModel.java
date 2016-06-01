/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Display model for OpenShift resources to allow
 * overriding equals and hash
 * @author jeff.cantrill
 * @author Jeff Maury
 *
 */
public class OpenShiftResourceUIModel extends ObservableUIPojo implements IResourceUIModel{

	private IResource resource;
	private String tostring;
	private final Object parent;
	private AtomicBoolean deleting = new AtomicBoolean(false);


	public OpenShiftResourceUIModel(IResource resource, Object parent){
		if(resource == null)
			throw new OpenShiftException("A null resource was passed while trying to create a display model");
		if(parent == null)
			throw new OpenShiftException("A null parent was passed while trying to create a display model");
		this.parent = parent;
		setResource(resource);
	}
	
	/**
	 * Returns a string representation for an Openshift resource.
	 * 
	 * @param resource the Openshift resource
	 * @return the string representation
	 */
	private String toString(IResource resource) {
	    return resource.getNamespace() + "/" + resource.getKind() + "/" + resource.getName() + "/" + resource.getResourceVersion();
	}
	
	@Override
	public Object getParent() {
		return this.parent;
	}

	/**
	 * Get the underlying resource for this display model.
	 * This will throw a cast exception if it can not be cast
	 * @return
	 */
	@Override
	public IResource getResource() {
		return this.resource;
	}
	
	protected void setResource(IResource resource) {
	    this.resource = resource;
	    this.tostring = toString(resource);
	}
	
    @Override
    public void setDeleting(boolean deleting) {
        this.deleting.set(deleting);
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.models.IResourceUIModel#isDeleting()
     */
    @Override
    public boolean isDeleting() {
        return deleting.get();
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		final String resourceVersion = resource == null ? "" : resource.getResourceVersion();
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((resourceVersion == null) ? 0 : resourceVersion.hashCode());
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
		OpenShiftResourceUIModel other = (OpenShiftResourceUIModel) obj;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		final String resourceVersion = resource.getResourceVersion(); 
		if (resourceVersion == null) {
			if (other.getResource().getResourceVersion() != null)
				return false;
		} else if (!resourceVersion.equals(other.getResource().getResourceVersion()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return tostring;
	}

	
	
	
}
