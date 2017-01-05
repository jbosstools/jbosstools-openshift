/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdapterFactory;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.model.IPod;

/**
 * This factory adapts ResourceWrapper for a running pod to IRunningPodHolder interface,
 * in this way allowing to filter running pods for menus.
 */
public class RunningPodHolderAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IRunningPodHolder.class) {
			final IResourceWrapper<?, IOpenshiftUIElement<?,?>> wrapper = Adapters.adapt(adaptableObject, IResourceWrapper.class);
			if(wrapper != null && wrapper.getWrapped() instanceof IPod 
					&& !ResourceUtils.isBuildPod((IPod)wrapper.getWrapped())) {
				return (T)new IRunningPodHolder() {
					@Override
					public IOpenshiftUIElement<?, IOpenshiftUIElement<?,?>> getPodUIElement() {
						return wrapper;
					}
				};
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{IRunningPodHolder.class};
	}

}
