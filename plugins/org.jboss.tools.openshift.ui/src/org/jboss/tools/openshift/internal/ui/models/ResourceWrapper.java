package org.jboss.tools.openshift.internal.ui.models;

import com.openshift.restclient.model.IResource;

public class ResourceWrapper extends AbstractResourceWrapper<IResource, AbstractResourceWrapper<?, ?>> {

	public ResourceWrapper(AbstractResourceWrapper<?, ?> parent, IResource resource) {
		super(parent, resource);
	}

}
