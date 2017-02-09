/*******************************************************************************
 * Copyright (c) 2017 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.internal.ui.models.IProjectWrapper;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class ResourceWrapperUtils {
	
	private ResourceWrapperUtils() {
	}

	public static <R extends IResource> List<R> getResources(Collection<IResourceWrapper<?, ?>> wrappers) {
		if (wrappers == null
			|| wrappers.isEmpty()) {
			return Collections.emptyList();
		}
		return wrappers.stream()
			.map(wrapper -> (R) wrapper.getWrapped())
			.collect(Collectors.toList());
	}

	public static IResource getResource(Object element) {
		IResource resource = null;
		if (element != null) {
			if (element instanceof IResource) {
				resource = (IResource) element;
			} else if (element instanceof IResourceWrapper) {
				resource = ((IResourceWrapper<?,?>) element) .getWrapped();
			}
		}
		return resource;
	}

	//Returns service UI element if given resource wrapper has a running pod and service wrapper as the parent, otherwise returns null.
	public static IServiceWrapper getServiceWrapperFor(IResourceWrapper<?,?> wrapper, Predicate<IResourceWrapper<?,?>> predicate) {
		if (wrapper != null) {
			Object parent = wrapper.getParent();
			if (parent instanceof IServiceWrapper) {
				return (IServiceWrapper) parent;
			} else if (parent instanceof IProjectWrapper) {
				 Optional<IResourceWrapper<?, ?>> firstWrapper =
						 ((IProjectWrapper) parent).getResourcesOfKind(ResourceKind.SERVICE).stream()
						.filter(predicate)
						.findFirst();
				 return (IServiceWrapper) firstWrapper.orElse(null);
			}
		}
		return null;
	}


}
