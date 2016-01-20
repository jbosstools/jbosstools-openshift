/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.util.Collection;

import com.openshift.restclient.model.IResource;

/**
 * The cache that contains a relation information
 * between resources.
 * 
 * @author jeff.cantrill
 *
 */
public interface IRelationCache {

	/**
	 * Retrieve the collection of resources of the given kind to
	 * the given resource. 
	 * @param resource
	 * @param targetKind
	 * @return the collection of related resources if known or an empty collection
	 */
	Collection<IResource> getResourcesFor(IResource resource, String targetKind);
}
