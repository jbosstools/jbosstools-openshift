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

import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.imageRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.core.Trace;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;

public class ObservableResourceCache implements IResourceCache {

	private Map<String, Collection<IBuildConfig>> imageRefToBuildConfigs = new ConcurrentHashMap<>();
	private Map<String, Collection<IDeploymentConfig>> imageRefToDeployConfigs = new ConcurrentHashMap<>();
	private Map<String, IResource> cache = new ConcurrentHashMap<>();
	private Set<IResourceCacheListener> listeners = Collections.synchronizedSet(new HashSet<>());
	
	@Override
	public void dispose() {
		flush();
	}

	@Override
	public synchronized void flush() {
		Collection<IResource> clone = new ArrayList<>(cache.values());
		clone.forEach(r->remove(r));
		imageRefToBuildConfigs.clear();
		imageRefToDeployConfigs.clear();
	}
	/**
	 * Key for caching an object
	 * 
	 * @param resource
	 * @return
	 */
	private String getCacheKey(IResource resource) {
		return getCacheKey(resource.getNamespace(), resource.getKind(), resource.getName());
	}
	
	private String getCacheKey(String namespace, String kind, String name) {
		return NLS.bind("{0}/{1}/{2}", new Object[] {namespace, kind, name});
	}
	
	@Override
	public <T extends IResource> Collection<T> getNamedResourcesByAnnotation(IResource resource, String kind, String annotation){
		if(resource != null && StringUtils.isNotBlank(kind) && StringUtils.isNotBlank(annotation)) {
			if(resource.isAnnotatedWith(annotation)) {
				final String name = resource.getAnnotation(annotation);
				return this.<T>getResourcesOf(kind).stream()
						.filter(r->name.equals(r.getName()))
						.collect(Collectors.toList());
			}
		}
		return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResource> T getCachedResource(String namespace, String kind, String name){
		return (T) cache.get(getCacheKey(namespace, kind, name));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResource> Collection<T> getResourcesOf(String kind) {
		return cache.values()
				.stream()
				.filter(r -> kind.equals(r.getKind()))
				.map(r -> (T) r)
				.collect(Collectors.toList());
	}
	
	@Override
	public Collection<IDeploymentConfig> getDeploymentConfigsBy(Collection<String> imageRefs) {
		return imageRefs.stream()
				.map(r->getDeploymentConfigsBy(r))
				.flatMap(l->l.stream())
				.collect(Collectors.toList());
	}
	
	@Override
	public Collection<IDeploymentConfig> getDeploymentConfigsBy(String imageRef) {
		synchronized (imageRefToDeployConfigs) {
			if(imageRefToDeployConfigs.containsKey(imageRef)) {
				return imageRefToDeployConfigs.get(imageRef);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * 
	 * @param resource
	 * @return true if cached; false otherwise
	 */
	public boolean add(IResource resource) {
		if(resource == null) return false;
		if(cache.containsKey(getCacheKey(resource))) {
			Trace.debug("-->Returning early since already processed {0}", resource);
			return false;
		}
		cache.put(getCacheKey(resource), resource);
		if(ResourceKind.DEPLOYMENT_CONFIG.equals(resource.getKind())) {
			cacheDeploymentConfigByImageChangeTrigger((IDeploymentConfig) resource);
		}
		if(ResourceKind.BUILD_CONFIG.equals(resource.getKind())) {
			cacheBuildConfigByOutput((IBuildConfig) resource);
		}
		synchronized (listeners) {
			try {
				listeners.forEach(l->l.handleAddToCache(this, resource));
			}catch(Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
		}
		return true;
	}
	
	private void cacheBuildConfigByOutput(IBuildConfig config) {
		String imageRef = imageRef(config);
		synchronized (imageRefToBuildConfigs) {
			if (!imageRefToBuildConfigs.containsKey(imageRef)) {
				imageRefToBuildConfigs.put(imageRef, Collections.synchronizedSet(new HashSet<>()));
			}
			imageRefToBuildConfigs.get(imageRef).add(config);
		}
	}

	private void cacheDeploymentConfigByImageChangeTrigger(IDeploymentConfig dc) {
		Collection<IDeploymentTrigger> triggers = dc.getTriggers().stream()
				.filter(t -> DeploymentTriggerType.IMAGE_CHANGE.equals(t.getType()))
				.collect(Collectors.toList());
		for (IDeploymentTrigger trigger : triggers) {
			String imageRef = imageRef((IDeploymentImageChangeTrigger) trigger);
			synchronized (imageRefToDeployConfigs) {
				if (!imageRefToDeployConfigs.containsKey(imageRef)) {
					imageRefToDeployConfigs.put(imageRef, Collections.synchronizedSet(new HashSet<>()));
				}
				imageRefToDeployConfigs.get(imageRef).add(dc);
			}
		}
	}

	/**
	 * 
	 * @param resource
	 * @return true if removed; false otherwise
	 */
	public boolean remove(IResource resource) {
		if(resource == null) return false;
		if(!cache.containsKey(getCacheKey(resource))) {
			Trace.debug("-->Returning early since dont know about {0}", resource);
			return false;
		}
		IResource old = cache.remove(getCacheKey(resource));
		if(old != null) {
			if(ResourceKind.DEPLOYMENT_CONFIG.equals(old.getKind())) {
				flushCacheDeploymentConfigByImageChangeTrigger((IDeploymentConfig) old);
			}
			if(ResourceKind.BUILD_CONFIG.equals(old.getKind())) {
				flushCacheBuildConfigByOutput((IBuildConfig) old);
			}
		}
		synchronized (listeners) {
			try {
				listeners.forEach(l->l.handleRemoveFromCache(this, resource));
			}catch(Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
		}
		return true;
	}

	private void flushCacheBuildConfigByOutput(IBuildConfig config) {
		String imageRef = imageRef(config);
		if (imageRefToBuildConfigs.containsKey(imageRef)) {
			synchronized (imageRefToBuildConfigs) {
				Collection<IBuildConfig> configs = imageRefToBuildConfigs.get(imageRef);
				if(configs != null) {
					configs.remove(config);
					if(configs.isEmpty()) {
						imageRefToBuildConfigs.remove(imageRef);
					}
				}
			}
		}
		
	}

	private void flushCacheDeploymentConfigByImageChangeTrigger(IDeploymentConfig dc) {
		Collection<IDeploymentTrigger> triggers = dc.getTriggers().stream()
				.filter(t -> DeploymentTriggerType.IMAGE_CHANGE.equals(t.getType()))
				.collect(Collectors.toList());
		for (IDeploymentTrigger trigger : triggers) {
			String imageRef = imageRef((IDeploymentImageChangeTrigger) trigger);
			synchronized (imageRefToDeployConfigs) {
				if(imageRefToDeployConfigs.containsKey(imageRef)) {
					Collection<IDeploymentConfig> configs = imageRefToDeployConfigs.get(imageRef);
					if(configs != null) {
						configs.remove(dc);
						if(configs.isEmpty()) {
							imageRefToDeployConfigs.remove(imageRef);
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param resource
	 * @return true if updated; false otherwise
	 */
	public boolean update(IResource resource) {
		if(resource == null) return false;
		if(alreadyProcessedResource(resource)) {
			Trace.debug("-->Returning early since already have this change: {0}", resource);
			return false;
		}
		IResource old = cache.put(getCacheKey(resource), resource);
		if(old != null) {
			if(ResourceKind.DEPLOYMENT_CONFIG.equals(old.getKind())) {
				flushCacheDeploymentConfigByImageChangeTrigger((IDeploymentConfig) old);
				cacheDeploymentConfigByImageChangeTrigger((IDeploymentConfig)resource);
			}
			if(ResourceKind.BUILD_CONFIG.equals(old.getKind())) {
				flushCacheBuildConfigByOutput((IBuildConfig) old);
				cacheBuildConfigByOutput((IBuildConfig)resource);
			}
		}
		synchronized (listeners) {
			try {
				listeners.forEach(l->l.handleUpdateToCache(this, resource));
			}catch(Exception e) {
				Trace.error("Exception while trying to notify cache listener", e);
			}
		}
		return true;
	}
	
	private boolean alreadyProcessedResource(IResource resource) {
		final String cacheKey = getCacheKey(resource);
		return cache.containsKey(cacheKey) && Integer.parseInt(cache.get(cacheKey).getResourceVersion()) >= Integer.parseInt(resource.getResourceVersion());
	}
	
	

	@Override
	public void addListener(IResourceCacheListener listener) {
		synchronized (listeners) {
			if(!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	@Override
	public void removeListener(IResourceCacheListener listener) {
		synchronized (listeners) {
			if(listeners.contains(listener)) {
				listeners.remove(listener);
			}
		}
	}
}
