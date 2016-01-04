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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;

/**
 * Figures out the resources in a project associated
 * with a deployment
 * 
 * @author jeff.cantrill
 *
 */
public class DeploymentResourceMapper implements OpenShiftAPIAnnotations{
	
	private static final String DOCKER_IMAGE_KIND = "DockerImage";
	private static final String IMAGE_STREAM_IMAGE_KIND = "ImageStreamImage";
	private static final String IMAGE_STREAM_TAG_KIND = "ImageStreamTag";
	private static final String RELATION_DELIMITER = "->";
	private static final Map<String, String[]> RELATIONSHIP_TYPE_MAP = new HashMap<>();
	
	private IProject project;
	private Connection conn;
	private List<Deployment> deployments = new ArrayList<>();
	private Map<IResource, Collection<Deployment>> resourceToDeployments = new ConcurrentHashMap<>();
	private Map<String, Collection<IDeploymentConfig>> imageRefToDeployConfigs;
	private Map<String, Collection<IResource>> relationMap = new ConcurrentHashMap<>();
	private Map<String, IResource> cache = new ConcurrentHashMap<>();

	static {
		RELATIONSHIP_TYPE_MAP.put(ResourceKind.BUILD, new String [] {
				ResourceKind.BUILD_CONFIG,
				ResourceKind.DEPLOYMENT_CONFIG,
				ResourceKind.POD
		});
	}

	public DeploymentResourceMapper(Connection conn, IProject project) {
		this.project = project;
		this.conn = conn;
		
		List<IReplicationController> rcs = load(ResourceKind.REPLICATION_CONTROLLER);
		List<IService> services = load(ResourceKind.SERVICE);
		List<IBuild> builds = load(ResourceKind.BUILD);
		List<IPod> pods = load(ResourceKind.POD);
		List<IDeploymentConfig> deployConfigs = load(ResourceKind.DEPLOYMENT_CONFIG);
		List<IBuildConfig> buildConfigs = load(ResourceKind.BUILD_CONFIG);
		
		imageRefToDeployConfigs = mapImageRefToDeployConfigs(deployConfigs);

		mapBuildsToDeploymentConfigs(builds);
		mapChildToParent(pods, rcs, DEPLOYMENT_NAME);
		mapChildToParent(rcs, deployConfigs, DEPLOYMENT_CONFIG_NAME);
		mapChildToParent(pods, builds, BUILD_NAME);
		mapChildToParent(builds, buildConfigs, BUILD_CONFIG_NAME);

		

		Map<String, Collection<IRoute>> serviceToRoutes = mapServiceToRoutes(project.getName());

		//serice by name->deployment  join selector
		//service by name ->dc->deployment
		for (IService service : services) {
			Deployment deployment = new Deployment(service);
			List<IPod> servicePods = selectPodsForService(service, pods);
			List<IBuild> appBuilds = getBuildsForDeployment(pods, builds);
			List<IResource> appBuildConfigs = getBuildConfigsForBuilds(appBuilds);
			
			deployment.setBuildResources(appBuilds);
			deployment.setPodResources(servicePods);
			deployment.setRouteResources(serviceToRoutes.get(service.getName()));
			deployment.setBuildConfigResources(appBuildConfigs);
			
			mapResourcesFor(deployment);
			deployments.add(deployment);
		}
	}
	private List<IResource> getBuildConfigsForBuilds(Collection<IBuild> builds){
		List<IResource> buildConfigs = new ArrayList<>();
		for (IBuild build : builds) {
			Collection<IResource> configs = relationMap.get(getKey(build, ResourceKind.BUILD_CONFIG));
			if(configs != null) {
				buildConfigs.addAll(configs);
			}
		}
		return buildConfigs;
	}
	
	private <T extends IResource> List<T> load(String kind){
		List<T> resources = conn.getResources(kind, project.getName());
		resources.forEach(r->cache.put(getCacheKey(r), r));
		return resources;
	}
	
	private void mapBuildsToDeploymentConfigs(Collection<IBuild> builds) {
		builds.forEach(build->mapBuildToDeploymentConfig(build));
	}
	
	private void mapBuildToDeploymentConfig(IBuild build) {
		String imageRef = imageRef(build, project);
		Collection<IDeploymentConfig> deploymentConfigs = imageRefToDeployConfigs.get(imageRef);
		if(deploymentConfigs != null) {
			deploymentConfigs.forEach(dc->createRelation(build, dc));
		}
	}

	private void mapChildToParent(Collection<? extends IResource> manys, Collection<? extends IResource> ones, String annotation) {
		mapChildToParent(manys, ones, annotation, false);
	}
	/*
	 * Create a mapping between resources based on an annotation
	 * (e.g. pod-> resourcecontroller).  Mapping key is KIND::NAME
	 */
	private void mapChildToParent(Collection<? extends IResource> manys, Collection<? extends IResource> ones, String annotation, boolean annotationKeyIsLabel) {
		Map<String, IResource> parents = ones.stream().collect(Collectors.toMap(IResource::getName, Function.identity()));
		for (IResource child : manys) {
			String parentName = annotationKeyIsLabel ? child.getLabels().get(annotation) : child.getAnnotation(annotation);
			if(parents.containsKey(parentName)) {
				IResource parent = parents.get(parentName);
				createRelation(child, parent);
			}
		}
	}
	
	/**
	 * Key for caching an object
	 * @param resource
	 * @return
	 */
	private String getCacheKey(IResource resource) {
		return NLS.bind("{0}::{1}",resource.getName(),resource.getKind());
	}
	
	/**
	 * The key for mapping relationships
	 * @param resource
	 * @param targetKind
	 * @return
	 */
	private String getKey(IResource resource, String targetKind) {
		return getKey(resource.getName(), resource.getKind(), targetKind);
	}

	/**
	 * The key for mapping relationships
	 * @param resource
	 * @param targetKind
	 * @return
	 */
	private String getKey(String name, String sourceKind, String targetKind) {
		return NLS.bind("{0}::{1}{2}{3}", new Object [] {name, sourceKind, RELATION_DELIMITER, targetKind});
	}
	
	private void createRelation(IResource start, IResource end) {
		String startKey = getKey(start, end.getKind());
		if(!relationMap.containsKey(startKey)) {
			relationMap.put(startKey, Collections.synchronizedList(new ArrayList<>()));
		}
		relationMap.get(startKey).add(end);
		String endKey = getKey(end, start.getKind());
		if(!relationMap.containsKey(endKey)) {
			relationMap.put(endKey, Collections.synchronizedList(new ArrayList<>()));
		}
		relationMap.get(endKey).add(start);
	}

	private List<IPod> selectPodsForService(IService service, List<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return pods.stream()
			.filter(p->selectorsOverlap(serviceSelector, p.getLabels()))
			.collect(Collectors.toList());
	}
	
	private boolean selectorsOverlap(Map<String, String> source, Map<String, String> target) {
		if(!target.keySet().containsAll(source.keySet())) {
			return false;
		}
		for (String key : source.keySet()) {
			if(!target.get(key).equals(source.get(key))) {
				return false;
			}
		}
		return true;
	}

	private void mapResourcesFor(Deployment deployment) {
		mapResourcesToDeployment(deployment.getBuilds(), deployment);
		mapResourcesToDeployment(deployment.getPods(), deployment);
		mapResourcesToDeployment(deployment.getReplicationControllers(), deployment);
		mapResourcesToDeployment(deployment.getRoutes(), deployment);
	}

	private void mapResourcesToDeployment(Collection<IResourceUIModel> resources, Deployment deployment) {
		for (IResourceUIModel model : resources) {
			mapResourceToDeployment(model.getResource(), deployment);
		}
	}

	private void mapResourceToDeployment(IResource resource, Deployment deployment) {
		if(!resourceToDeployments.containsKey(resource)) {
			resourceToDeployments.put(resource, Collections.synchronizedList(new ArrayList<>()));
		}
		resourceToDeployments.get(resource).add(deployment);
	}

	public Collection<Deployment> getDeployments(){
		return deployments;
	}

	
	private Map<String, Collection<IRoute>> mapServiceToRoutes(String projectName){
		List<IRoute> routes = conn.getResources(ResourceKind.ROUTE, project.getName());
		Map<String, Collection<IRoute>> map = new HashMap<String, Collection<IRoute>>(routes.size());
		for (IRoute route : routes) {
			String service = route.getServiceName();
			if(!map.containsKey(service)) {
				map.put(service, Collections.synchronizedSet(new HashSet<>()));
			}
			map.get(service).add(route);
		}
		return map;
    }
	private Map<IService, List<IReplicationController>> mapServicesToRepControllers(Collection<IService> services, Collection<IReplicationController> rcs){
		Map<IService, List<IReplicationController>> map = new HashMap<>(services.size());
		for (IReplicationController rc : rcs) {
			Map<String, String> deploymentSelector = rc.getReplicaSelector();
			for (IService service : services) {
				Map<String, String> serviceSelector = service.getSelector();
				if(selectorsJoin(serviceSelector, deploymentSelector)) {
					if(!map.containsKey(service)) {
						map.put(service, Collections.synchronizedList(new ArrayList<>()));
					}
					map.get(service).add(rc);
				}
			}		
		}
		return map;
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @return true if target includes all source keys and values; false otherwise
	 */
	private boolean selectorsJoin(Map<String, String> source, Map<String, String> target) {
		if(!target.keySet().containsAll(source.keySet())) {
			return false;
		}
		for (String key : source.keySet()) {
			if(!target.get(key).equals(source.get(key))) {
				return false;
			}
		}
		return true;
	}
	
	private List<IBuild> getBuildsForDeployment(List<IPod> pods, List<IBuild> builds) {
		List<IBuild> buildsForDeployment = new ArrayList<IBuild>();
		for (IBuild build : builds) {
			String buildImageRef = imageRef(build, this.project);
			if(imageRefToDeployConfigs.containsKey(buildImageRef)) {
				buildsForDeployment.add(build);
			}
		}
		return buildsForDeployment;
	}
	
	private Map<String, Collection<IDeploymentConfig>> mapImageRefToDeployConfigs(Collection<IDeploymentConfig> configs){
		Map<String, Collection<IDeploymentConfig>> map = new ConcurrentHashMap<>();
		for (IDeploymentConfig dc : configs) {
			List<IDeploymentTrigger> imageChangeTriggers = filterImageChangeTriggers(dc);
			for (IDeploymentTrigger trigger : imageChangeTriggers) {
				String imageRef = imageRef((IDeploymentImageChangeTrigger) trigger,  this.project);
				if(!map.containsKey(imageRef)) {
					map.put(imageRef, Collections.synchronizedList(new ArrayList<>()));
				}
				map.get(imageRef).add(dc);
			}
		}
		return map;
	}
	
	private String imageRef(IBuild build, IProject project) {
		final String kind = build.getOutputKind();
		if(IMAGE_STREAM_TAG_KIND.equals(kind) || IMAGE_STREAM_IMAGE_KIND.equals(kind)) {
			return new DockerImageURI("", project.getName(),build.getOutputTo().getNameAndTag()).toString();
		}
		if(DOCKER_IMAGE_KIND.equals(kind)) {
			return build.getOutputTo().getNameAndTag().toString();
		}
		return "";
		
	}
	
	private String imageRef(IDeploymentImageChangeTrigger trigger, IProject project) {
		final String kind = trigger.getKind();
		if(IMAGE_STREAM_TAG_KIND.equals(kind) || IMAGE_STREAM_IMAGE_KIND.equals(kind)) {
			return new DockerImageURI("", project.getName(),trigger.getFrom().getNameAndTag()).toString();
		}
		if(DOCKER_IMAGE_KIND.equals(kind)) {
			return trigger.getFrom().getNameAndTag().toString();
		}
		return "";
	}

	private List<IDeploymentTrigger>  filterImageChangeTriggers(IDeploymentConfig dc){
		return dc.getTriggers()
				.stream().
				filter(t->t.getType().equals(DeploymentTriggerType.IMAGE_CHANGE)).collect(Collectors.toList());
	}

	public synchronized void add(IResource resource) {
		try {
			Trace.debug("Trying to add resource to deployment {0}", resource);
			switch(resource.getKind()) {
			case ResourceKind.BUILD:
				mapBuildToDeploymentConfig((IBuild) resource);
			}
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if(deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					deployment.add(resource);
					mapResourceToDeployment(resource, deployment);
				}
			}
			cache.put(getCacheKey(resource), resource);
		}catch(Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	public synchronized void remove(IResource resource) {
		try {
			Trace.debug("Trying to remove resource to deployment {0}", resource);
			switch(resource.getKind()) {
			case ResourceKind.BUILD:
			}
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if(deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					resourceToDeployments.remove(resource);
					deployment.remove(resource);
				}
			}
			if(RELATIONSHIP_TYPE_MAP.containsKey(ResourceKind.BUILD)) {
				for (String kind : RELATIONSHIP_TYPE_MAP.get(ResourceKind.BUILD)) {
					String left = getKey(resource, kind);
					if(relationMap.containsKey(left)) {
						for (IResource target: relationMap.get(left)) {
							String right = getKey(target, resource.getKind());
							if(relationMap.containsKey(right)) {
								relationMap.get(right).remove(resource);
							}
						}
						relationMap.remove(left);
					}
				}
			}
			cache.remove(getCacheKey(resource));
		}catch(Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	public synchronized void update(IResource resource) {
		try {
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if(deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					deployment.update(resource);
				}
			}
		}catch(Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	private Collection<Deployment> findDeploymentsFor(IResource resource) {
		Trace.debug("Looking for deployment associated with: {0}", resource);
		//build->dc->rc->pod->d
		if(resourceToDeployments.containsKey(resource)) {
			return resourceToDeployments.get(resource);
		}
		String path = null;
		switch(resource.getKind()) {
		case ResourceKind.BUILD:
			path = ResourceKind.DEPLOYMENT_CONFIG;
			break;
		case ResourceKind.DEPLOYMENT_CONFIG:
			path = ResourceKind.REPLICATION_CONTROLLER;
			break;
		case ResourceKind.REPLICATION_CONTROLLER:
			path = ResourceKind.POD;
			break;
		case ResourceKind.POD:
		default:
		}
		if(path != null) {
			String key = getKey(resource, path);
			if(relationMap.containsKey(key) && relationMap.get(key) != null) {
				Collection<Deployment> deployments = new ArrayList<>();
				for (IResource relation : relationMap.get(key)) {
					deployments.addAll(findDeploymentsFor(relation));
				}
				return deployments;
			}
		}
		return Collections.emptyList();
	}

}
