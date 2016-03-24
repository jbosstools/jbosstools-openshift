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

import org.eclipse.ui.services.IDisposable;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;

public interface IResourceCache extends IDisposable {
	
	/**
	 * 
	 * @param namespace
	 * @param kind
	 * @param name
	 * @return the cached resource or null if not found
	 */
	<T extends IResource> T getCachedResource(String namespace, String kind, String name);
	
	/**
	 * @param kind the kind of resources to return
	 * @return the OpenShift {@link IResource} of the given {@code kind}.
	 */
	<T extends IResource> Collection<T> getResourcesOf(String kind);
	
	/**
	 * Retrieve a collection of resources that are explicitly named by a given annotation
	 * 
	 * @param resource
	 * @param kind  the target kind that names 'resource' by the given annotation (e.g build pod->build)
	 * @param annotation  
	 * @return the collection of resources
	 */
	<T extends IResource> Collection<T> getNamedResourcesByAnnotation(IResource resource, String kind, String annotation);
	
	/**
	 * The collection of deploymentconfigs for the given
	 * image ref
	 * @param imageRef
	 * @return
	 */
	Collection<IDeploymentConfig> getDeploymentConfigsBy(String imageRef);
	
	Collection<IDeploymentConfig> getDeploymentConfigsBy(Collection<String> imageRefs);

	/**
	 * @param resource
	 * @return true if cached; false otherwise
	 */
	boolean add(IResource resource);
	
	/**
	 * @param resource
	 * @return true if removed; false otherwise
	 */
	boolean remove(IResource resource);
	
	/**
	 * @param resource
	 * @return true if updated; false otherwise
	 */
	boolean update(IResource resource);
	
	void flush();
	
	void addListener(IResourceCacheListener listener);
	
	void removeListener(IResourceCacheListener listener);

	static interface IResourceCacheListener {
		
		void handleAddToCache(IResourceCache cache, IResource resource);
		void handleRemoveFromCache(IResourceCache cache, IResource resource);
		void handleUpdateToCache(IResourceCache cache, IResource resource);
		
	}
}
