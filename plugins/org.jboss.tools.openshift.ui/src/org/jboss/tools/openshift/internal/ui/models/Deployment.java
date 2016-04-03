/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.containsAll;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.imageRef;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isBuildPod;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IAdaptable;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.models.IResourceCache.IResourceCacheListener;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.route.IRoute;

/**
 * A deployment is the collection of resources
 * that makes up an 'application'
 * 
 * @author jeff.cantrill
 *
 */
public class Deployment extends ResourcesUIModel 
		implements IResourceUIModel, IAdaptable, IResourceCacheListener, OpenShiftAPIAnnotations {

	private final IService service;
	private final IProject project;
	private final Object grandParent; //connection
	
	public Deployment(IService service, IProjectAdapter parent) {
		super(parent);
		this.service = service;
		this.project = parent.getProject();
		this.grandParent = parent.getParent();
	}
	
	public IService getService() {
		return this.service;
	}
	
	@Override
	public IResource getResource() {
		return getService();
	}

	@Override
	public Collection<IResourceUIModel> getServices() {
		return Collections.emptyList();
	}

	@Override
	public void setServices(Collection<IResourceUIModel> services) {
	}

	@Override
	public void setServiceResources(Collection<IResource> services) {
	}
	
	@Override
	public void handleAddToCache(IResourceCache cache, IResource resource) {
		switch(resource.getKind()) {
			case ResourceKind.BUILD:
				addOrUpdateResources(handleBuild(cache, (IBuild) resource));
				break;
			case ResourceKind.BUILD_CONFIG:
				addOrUpdateResources(handleBuildConfig(cache, (IBuildConfig) resource));
				break;
			case ResourceKind.ROUTE:
				handleRoute(cache, (IRoute) resource);
				break;
			case ResourceKind.REPLICATION_CONTROLLER:
				handleRepController(cache, (IReplicationController) resource);
				break;
			case ResourceKind.DEPLOYMENT_CONFIG:
				addOrUpdateResources(handleDeploymentConfig(cache, (IDeploymentConfig) resource));
				break;
			case ResourceKind.IMAGE_STREAM_TAG:
				addOrUpdateResources(handleImageStreamTag(cache, resource));
				break;
			case ResourceKind.POD:
			{
				IPod pod = (IPod) resource;
				if(isBuildPod(pod)) {
					addOrUpdateResources(handleBuildPod(cache, pod));
				}else {
					handlePod(cache, pod);
				}
			}
			default:
		}
	}

	private Collection<IResource> handleBuildConfig(IResourceCache cache, IBuildConfig config) {
		Collection<IResource> resources = new HashSet<IResource>();
		final String imageRef = imageRef(config);
		Collection<IDeploymentConfig> dcs = cache.getDeploymentConfigsBy(imageRef).stream()
				.filter(dc->containsAll(service.getSelector(), dc.getReplicaSelector()))
				.collect(Collectors.toList());
		if(!dcs.isEmpty()) {
			resources.add(config);
			resources.addAll(dcs);
			
			final String bcName = config.getName();
			Collection<IBuild> builds = cache.<IBuild>getResourcesOf(ResourceKind.BUILD).stream()
				.filter(b->b.isAnnotatedWith(BUILD_CONFIG_NAME) && bcName.equals(b.getAnnotation(BUILD_CONFIG_NAME)))
				.collect(Collectors.toList());
			resources.addAll(builds);
			
			Collection<String> buildNames = builds.stream().map(b->b.getName()).collect(Collectors.toList());
			
			Collection<IPod> buildPods = cache.<IPod>getResourcesOf(ResourceKind.POD).stream()
					.filter(p->isBuildPod(p) && buildNames.contains(p.getAnnotation(BUILD_NAME)))
					.collect(Collectors.toList());
			resources.addAll(buildPods);
			
			Collection<IResource> istags = cache.getResourcesOf(ResourceKind.IMAGE_STREAM_TAG).stream()
				.filter(tag->imageRef.equals(tag.getName()))
				.collect(Collectors.toList());
			resources.addAll(istags);
		}
		return resources;
	}
	
	private Collection<IResource> handleImageStreamTag(IResourceCache cache, IResource istag) {
		final String isTagName = istag.getName();
		Collection<IResource> resources = new HashSet<IResource>();
		Collection<IDeploymentConfig> dcs = cache.getDeploymentConfigsBy(isTagName).stream()
				.filter(dc->containsAll(service.getSelector(), dc.getReplicaSelector()))
				.collect(Collectors.toList());
		if(!dcs.isEmpty()) {
			resources.add(istag);
			resources.addAll(dcs);
			
			Collection<IBuildConfig> bcs = cache.<IBuildConfig>getResourcesOf(ResourceKind.BUILD_CONFIG).stream()
				.filter(b->isTagName.equals(imageRef(b)))
				.collect(Collectors.toList());
			resources.addAll(bcs);
			
			if(!bcs.isEmpty()) {
				Collection<String> bcNames = bcs.stream().map(b->b.getName()).collect(Collectors.toList());
				
				Collection<IBuild> builds = cache.<IBuild>getResourcesOf(ResourceKind.BUILD).stream()
					.filter(b->b.isAnnotatedWith(BUILD_CONFIG_NAME) && bcNames.contains(b.getAnnotation(BUILD_CONFIG_NAME)))
					.collect(Collectors.toList());
				resources.addAll(builds);
				
				if(!builds.isEmpty()) {
					Collection<String> buildNames = builds.stream().map(b->b.getName()).collect(Collectors.toList());
					Collection<IPod> buildPods = cache.<IPod>getResourcesOf(ResourceKind.POD).stream()
							.filter(p->isBuildPod(p) && buildNames.contains(p.getAnnotation(BUILD_NAME)))
							.collect(Collectors.toList());
					
					resources.addAll(buildPods);
				}
			}
		}
		return resources;
	}

	private Collection<IResource> handleBuild(IResourceCache cache, IBuild build) {
		Collection<IResource> resources = new HashSet<IResource>();
		final String imageRef = imageRef(build);
		Collection<IDeploymentConfig> dcs = cache.getDeploymentConfigsBy(imageRef).stream()
				.filter(dc->containsAll(service.getSelector(), dc.getReplicaSelector()))
				.collect(Collectors.toList());
		if(!dcs.isEmpty()) {
			resources.add(build);
			resources.addAll(dcs);
			
			Collection<IBuildConfig> buildConfigs = cache.getNamedResourcesByAnnotation(build, ResourceKind.BUILD_CONFIG, BUILD_CONFIG_NAME);
			resources.addAll(buildConfigs);
			
			final String buildName = build.getName();
			Collection<IPod> buildPods = cache.<IPod>getResourcesOf(ResourceKind.POD).stream()
				.filter(p->isBuildPod(p) && buildName.equals(p.getAnnotation(BUILD_NAME)))
				.collect(Collectors.toList());
			resources.addAll(buildPods);
			
			
			Collection<String> bcImageRefs = buildConfigs.stream().map(b->imageRef(b)).collect(Collectors.toList());
			Collection<IResource> istags = cache.getResourcesOf(ResourceKind.IMAGE_STREAM_TAG).stream()
					.filter(tag->bcImageRefs.contains(tag.getName()))
					.collect(Collectors.toList());
			resources.addAll(istags);
		}
		return resources;
	}
	
	private Collection<IResource> handleBuildPod(IResourceCache cache, IPod pod) {
		Collection<IResource> resources = new HashSet<IResource>();
		Collection<IBuild> builds = cache.getNamedResourcesByAnnotation(pod, ResourceKind.BUILD, BUILD_NAME);
		
		Collection<String> buildImageRefs = builds.stream().map(b->imageRef(b)).collect(Collectors.toList());
		Collection<IDeploymentConfig> dcs = cache.getDeploymentConfigsBy(buildImageRefs).stream()
				.filter(dc->containsAll(service.getSelector(), dc.getReplicaSelector()))
				.collect(Collectors.toList());
		if(!dcs.isEmpty()) {
			resources.add(pod);
			resources.addAll(dcs);
			resources.addAll(builds); //filter here based on what was matched?
			for (IBuild build : builds) {
				Collection<IBuildConfig> bcs = cache.getNamedResourcesByAnnotation(build, ResourceKind.BUILD_CONFIG, BUILD_CONFIG_NAME);
				resources.addAll(bcs);
				
				Collection<String> bcImageRefs = bcs.stream().map(b->imageRef(b)).collect(Collectors.toList());
				Collection<IResource> istags = cache.getResourcesOf(ResourceKind.IMAGE_STREAM_TAG).stream()
						.filter(tag->bcImageRefs.contains(tag.getName()))
						.collect(Collectors.toList());
				resources.addAll(istags);
			}
		}
		return resources;
	}
	
	private Collection<String> getDeploymentConfigImageRefs(IDeploymentConfig dc){
		return getDeploymentConfigImageRefs(Arrays.asList(dc));
	}
	
	private Collection<String> getDeploymentConfigImageRefs(Collection<IDeploymentConfig> dcs){
		return dcs.stream()
				.map(dc->dc.getTriggers())
				.flatMap(l->l.stream())
				.filter(t -> t.getType().equals(DeploymentTriggerType.IMAGE_CHANGE))
				.map(t->imageRef((IDeploymentImageChangeTrigger)t))
				.collect(Collectors.toList());
	}

	private void handlePod(IResourceCache cache, IPod pod) {
		if(containsAll(service.getSelector(), pod.getLabels())) {
			add(pod);
			addResourcesByAnnotation(cache, pod, ResourceKind.REPLICATION_CONTROLLER, OpenShiftAPIAnnotations.DEPLOYMENT_NAME);
			addResourcesByAnnotation(cache, pod, ResourceKind.DEPLOYMENT_CONFIG, OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
		}
	}
	
	private Collection<IResource> handleDeploymentConfig(IResourceCache cache, IDeploymentConfig dc) {
		Collection<IResource> resources = new HashSet<IResource>();
		if(containsAll(service.getSelector(), dc.getReplicaSelector())) {
			resources.add(dc);
			
			Collection<String> imageRefs = getDeploymentConfigImageRefs(dc);
			
			//find associated resources through buildconfigs
			Collection<IBuildConfig> bcs = cache.<IBuildConfig>getResourcesOf(ResourceKind.BUILD_CONFIG).stream()
				.filter(b->imageRefs.contains(imageRef(b)))
				.collect(Collectors.toList());
			resources.addAll(bcs);
			
			if(!bcs.isEmpty()) {
				
				Collection<String> bcNames = bcs.stream().map(b->b.getName()).collect(Collectors.toList());
				
				Collection<IBuild> builds = cache.<IBuild>getResourcesOf(ResourceKind.BUILD).stream()
					.filter(b->b.isAnnotatedWith(BUILD_CONFIG_NAME) && bcNames.contains(b.getAnnotation(BUILD_CONFIG_NAME)))
					.collect(Collectors.toList());
				resources.addAll(builds);
				
				if(!builds.isEmpty()) {
					Collection<String> buildNames = builds.stream().map(b->b.getName()).collect(Collectors.toList());
					Collection<IPod> buildPods = cache.<IPod>getResourcesOf(ResourceKind.POD).stream()
							.filter(p->isBuildPod(p) && buildNames.contains(p.getAnnotation(BUILD_NAME)))
							.collect(Collectors.toList());
					
					resources.addAll(buildPods);
				}
			}
		}
		return resources;
	}

	private void handleRepController(IResourceCache cache, IReplicationController resource) {
		if(containsAll(service.getSelector(), resource.getReplicaSelector()) && !hasModelFor(resource)) {
			add(resource);
			addResourcesByAnnotation(cache, resource, ResourceKind.DEPLOYMENT_CONFIG, OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
		}
	}

	private void handleRoute(IResourceCache cache, IRoute route) {
		if(ResourceUtils.areRelated(route, service) && !hasModelFor(route)) {
			add(route);
		}
	}
	
	private void addResourcesByAnnotation(IResourceCache cache, IResource resource, String kind, String annotation) {
		if(resource.isAnnotatedWith(annotation)) {
			Collection<IResource> resources = cache.getResourcesOf(kind);
			String name = resource.getAnnotation(annotation);
			resources.forEach(r->{
				if(name.equals(r.getName())) {
					add(r);
				}
			});
		}
	}

	@Override
	public void handleRemoveFromCache(IResourceCache cache, IResource resource) {
		remove(resource);
	}

	@Override
	public void handleUpdateToCache(IResourceCache cache, IResource resource) {
		switch(resource.getKind()) {
			case ResourceKind.BUILD:
			{
				Collection<IResource> resources = handleBuild(cache, (IBuild) resource);
				addOrUpdateResources(resources);
				break;
				
			}
			case ResourceKind.BUILD_CONFIG:
			{
				addOrUpdateResources(handleBuildConfig(cache, (IBuildConfig) resource));
				break;
			}
			case ResourceKind.DEPLOYMENT_CONFIG:
				addOrUpdateResources(handleDeploymentConfig(cache, (IDeploymentConfig) resource));
				break;
			case ResourceKind.IMAGE_STREAM_TAG:
				addOrUpdateResources(handleImageStreamTag(cache, resource));
				break;
			case ResourceKind.POD:
			{
				IPod pod = (IPod) resource;
				if(isBuildPod(pod)) {
					Collection<IResource> resources = handleBuildPod(cache, pod);
					addOrUpdateResources(resources);
					break;
				}
			}
			default:
				update(resource);
		}

	}
	private void addOrUpdateResources(Collection<IResource> resources) {
		for (IResource r : resources) {
			if(hasModelFor(r)) {
				update(r);
			}else {
				add(r);
			}
			
		}
	}

	@Override
	public String toString() {
		return service.getNamespace() + "/" + service.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IBuildConfig.class.equals(adapter)) {
			Collection<IResourceUIModel> buildConfigs = getBuildConfigs();
			if (buildConfigs.size() == 1) {
				return (T) buildConfigs.iterator().next().getResource();
			}
		} else if (IService.class.equals(adapter) || IResource.class.equals(adapter)) {
			return (T) getService();
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((grandParent == null) ? 0 : grandParent.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
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
		Deployment other = (Deployment) obj;
		if (grandParent == null) {
			if (other.grandParent != null)
				return false;
		} else if (!grandParent.equals(other.grandParent))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		return true;
	}

	
}

