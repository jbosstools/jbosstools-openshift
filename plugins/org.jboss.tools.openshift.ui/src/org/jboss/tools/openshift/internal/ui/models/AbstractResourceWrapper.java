package org.jboss.tools.openshift.internal.ui.models;

import com.openshift.restclient.model.IResource;

public abstract class AbstractResourceWrapper<R extends IResource, P extends IOpenshiftUIElement<?>>
		extends AbstractOpenshiftUIElement<R, P> {

	public AbstractResourceWrapper(P parent, R resource) {
		super(parent, resource);
	}

	public IResource getResource() {
		return getWrapped();
	}

	@SuppressWarnings("unchecked")
	public void updateWith(IResource r) {
		if (OpenshiftUIModel.isOlder(getResource(), r)) {
			super.updateWith((R) r);
		}
	}

}
