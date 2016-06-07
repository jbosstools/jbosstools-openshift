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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.core.Trace;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

public class ResourceCache {
	private Map<String, Map<String, IResource>> cache = new HashMap<String, Map<String, IResource>>();

	public void dispose() {
		flush();
	}

	public void flush() {
		synchronized (cache) {
			cache.clear();
		}
	}

	public void flush(String namespace) {
		synchronized (cache) {
			cache.remove(namespace);
		}
	}

	/**
	 * Key for caching an object
	 * 
	 * @param resource
	 * @return
	 */
	private String getCacheKey(IResource resource) {
		return getCacheKey(resource.getKind(), resource.getName());
	}

	private String getCacheKey(String kind, String name) {
		return NLS.bind("{0}/{1}", new Object[] { kind, name });
	}

	@SuppressWarnings("unchecked")
	public <T extends IResource> T getResource(String namespace, String kind, String name) {
		synchronized (cache) {
			Map<String, IResource> projectResources = cache.get(namespace);
			if (projectResources != null) {
				return (T) projectResources.get(getCacheKey(kind, name));
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends IResource> Collection<T> getResources(String namespace, String kind) {
		synchronized (cache) {

			Map<String, IResource> projectResources = cache.get(namespace);
			if (projectResources != null) {
				return projectResources.values().stream().filter(r -> kind.equals(r.getKind())).map(r -> (T) r)
						.collect(Collectors.toList());
			}
			return Collections.emptyList();
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends IResource> Collection<T> getResources(String namespace) {
		synchronized (cache) {
			Map<String, IResource> projectResources = cache.get(namespace);
			if (projectResources != null) {
				return new ArrayList<T>((Collection<T>) projectResources.values());
			}
			return Collections.emptyList();
		}
	}

	/**
	 * 
	 * @param resource
	 * @return true if cached; false otherwise
	 */
	public boolean add(IResource resource) {
		if (resource == null)
			return false;
		synchronized (cache) {
			if (getCachedVersion(resource) != null) {
				Trace.debug("-->Returning early since already processed {0}", resource);
				return false;
			}
			putIntoCache(resource);
		}
		return true;
	}

	private void removeFromCache(IResource resource) {
		Map<String, IResource> projectResources = cache.get(getNamespace(resource));
		if (projectResources != null) {
			projectResources.remove(getCacheKey(resource));
			if (projectResources.isEmpty()) {
				cache.remove(getNamespace(resource));
			}
		}
	}

	private void putIntoCache(IResource resource) {
		Map<String, IResource> projectResources = cache.get(getNamespace(resource));
		if (projectResources == null) {
			projectResources = new HashMap<String, IResource>();
			cache.put(getNamespace(resource), projectResources);
		}
		projectResources.put(getCacheKey(resource), resource);
	}

	public String getNamespace(IResource resource) {
		return (resource instanceof IProject ? "" : resource.getNamespace());
	}

	public IResource getCachedVersion(IResource resource) {
		return getResource(getNamespace(resource), resource.getKind(), resource.getName());
	}

	/**
	 * 
	 * @param resource
	 * @return true if removed; false otherwise
	 */
	public boolean remove(IResource resource) {
		if (resource == null)
			return false;
		synchronized (cache) {
			removeFromCache(resource);
		}
		return true;

	}

	/**
	 * 
	 * @param resource
	 * @return true if updated; false otherwise
	 */
	public boolean update(IResource resource) {
		if (resource == null)
			return false;
		synchronized (cache) {
			if (isUpToDate(resource)) {
				Trace.debug("-->Returning early since already have this change: {0}", resource);
				return false;
			}
			putIntoCache(resource);
		}
		return true;
	}

	public boolean isUpToDate(IResource resource) {
		IResource cachedVersion = getCachedVersion(resource);
		return cachedVersion != null && Integer.parseInt(cachedVersion.getResourceVersion()) >= Integer
				.parseInt(resource.getResourceVersion());
	}
}
