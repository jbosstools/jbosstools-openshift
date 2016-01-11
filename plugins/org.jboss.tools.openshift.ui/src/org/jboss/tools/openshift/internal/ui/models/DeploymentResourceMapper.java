/*******************************************************************************
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * Copyright (c) 2015 Red Hat, Inc.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.selectorsOverlap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.Trace;
import org.jboss.tools.openshift.internal.core.WatchManager;
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
import com.openshift.restclient.model.IImageStream;
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
public class DeploymentResourceMapper implements OpenShiftAPIAnnotations, IRefreshable{
	
	private static enum State {
		UNINITIALIZED,
		LOADING,
		LOADED
	}
	private static final String DOCKER_IMAGE_KIND = "DockerImage";
	private static final String IMAGE_STREAM_IMAGE_KIND = "ImageStreamImage";
	private static final String IMAGE_STREAM_TAG_KIND = "ImageStreamTag";
	private static final String RELATION_DELIMITER = "->";
	private static final Map<String, String[]> RELATIONSHIP_TYPE_MAP = new HashMap<>();
	
	private final IProjectAdapter projectAdapter;
	private final IProject project;
	private final Connection conn;
	private List<Deployment> deployments = Collections.synchronizedList(new ArrayList<>());
	private Map<IResource, Collection<Deployment>> resourceToDeployments = new ConcurrentHashMap<>();
	private Map<String, Collection<IDeploymentConfig>> imageRefToDeployConfigs;
	private Map<String, Collection<IResource>> relationMap = new ConcurrentHashMap<>();
	private Map<String, IResource> cache = new ConcurrentHashMap<>();
	private AtomicReference<State> state = new AtomicReference<>(State.UNINITIALIZED);

	static {
		RELATIONSHIP_TYPE_MAP.put(ResourceKind.BUILD, new String [] {
				ResourceKind.BUILD_CONFIG,
				ResourceKind.DEPLOYMENT_CONFIG,
				ResourceKind.POD
		});
	}

	public DeploymentResourceMapper(Connection conn, IProjectAdapter projectAdapter) {
		this.projectAdapter = projectAdapter;
		this.project = projectAdapter.getProject();
		this.conn = conn;
	}
	
	
	@Override
	public synchronized void refresh() {
		WatchManager.getInstance().stopWatch(project);
		deployments.clear();
		for (@SuppressWarnings("rawtypes") Map map : new Map[] {resourceToDeployments, imageRefToDeployConfigs, relationMap, cache}) {
			map.clear();//need to fire events?
		}
		state.set(State.UNINITIALIZED);
		buildDeployments();
	}


	public IProjectAdapter getProjectAdapter() {
		return this.projectAdapter;
	}
	
	private synchronized void buildDeployments() {
		if(state.compareAndSet(State.UNINITIALIZED, State.LOADING)) {
			try {
				List<IReplicationController> rcs = load(ResourceKind.REPLICATION_CONTROLLER);
				List<IService> services = load(ResourceKind.SERVICE);
				List<IRoute> routes = load(ResourceKind.ROUTE);
				List<IBuild> builds = load(ResourceKind.BUILD);
				List<IPod> pods = load(ResourceKind.POD);
				List<IDeploymentConfig> deployConfigs = load(ResourceKind.DEPLOYMENT_CONFIG);
				List<IBuildConfig> buildConfigs = load(ResourceKind.BUILD_CONFIG);
				List<IImageStream> imageStreams = load(ResourceKind.IMAGE_STREAM);
				
				imageRefToDeployConfigs = mapImageRefToDeployConfigs(deployConfigs);
				
				mapChildToParent(pods, rcs, DEPLOYMENT_NAME);
				mapChildToParent(pods, deployConfigs, DEPLOYMENT_CONFIG_NAME);
				mapChildToParent(pods, builds, BUILD_NAME); //build pods	
				mapChildToParent(rcs, deployConfigs, DEPLOYMENT_CONFIG_NAME);
				mapChildToParent(builds, buildConfigs, BUILD_CONFIG_NAME, true);
				
				mapBuildConfigsToImageStreams(buildConfigs, imageStreams);
				mapBuildsToDeploymentConfigs(builds);
				mapServicesToRepControllers(services, rcs);
				mapServicesToRoutes(services, routes);
				
				for (IService service : services) {
					Deployment deployment = new Deployment(service);
					Collection<IPod> appPods = getPodsForService(service, pods);
					Collection<IResource> appRcs = getResourcesFor(service, ResourceKind.REPLICATION_CONTROLLER);
					Collection<IBuild> appBuilds = getBuildsForPods(appPods, builds);
					Collection<IResource> appBuildConfigs = getResourcesFor(appBuilds, ResourceKind.BUILD_CONFIG);
					
					deployment.setBuildResources(appBuilds);
					deployment.setPodResources(appPods);
					deployment.setRouteResources(getResourcesFor(service, ResourceKind.ROUTE));
					deployment.setBuildConfigResources(appBuildConfigs);
					deployment.setReplicationControllerResources(appRcs);
					deployment.setImageStreamResources(getResourcesFor(appBuildConfigs, ResourceKind.IMAGE_STREAM));
					deployment.setDeploymentConfigResources(getResourcesFor(appPods, ResourceKind.DEPLOYMENT_CONFIG));
					
					cacheDeployment(deployment);
				}
				WatchManager.getInstance().startWatch(project);
			}finally {
				state.set(State.LOADED);
			}
		}
	}
	
	public boolean isLoading() {
		return State.LOADING == state.get();
	}
	
	private void cacheDeployment(Deployment deployment) {
		deployments.add(deployment);
		mapResourcesFor(deployment);
	}
	
	private void mapResourcesFor(Deployment deployment) {
		mapResourcesToDeployment(deployment.getBuilds(), deployment);
		mapResourcesToDeployment(deployment.getPods(), deployment);
		mapResourcesToDeployment(deployment.getRoutes(), deployment);
		mapResourcesToDeployment(deployment.getBuildConfigs(), deployment);
		mapResourcesToDeployment(deployment.getReplicationControllers(), deployment);
		mapResourcesToDeployment(deployment.getImageStreams(), deployment);
		mapResourcesToDeployment(deployment.getDeploymentConfigs(), deployment);
	}

	private void mapResourcesToDeployment(Collection<IResourceUIModel> resources, Deployment deployment) {
		for (IResourceUIModel model : resources) {
			mapResourceToDeployment(model.getResource(), deployment);
		}
	}


	private void mapBuildConfigsToImageStreams(List<IBuildConfig> buildConfigs, List<IImageStream> is) {
		Map<String, IImageStream> tagsToStreams = new HashMap<>();
		for (IImageStream stream : is) {
			String name = stream.getName();
			Collection<String> tags = stream.getTagNames();
			for (String tag : tags) {
				String key = name + ":" + tag;
				tagsToStreams.put(key, stream);
			}
		}
		buildConfigs.forEach(bc-> createRelation(bc, tagsToStreams.get(bc.getBuildOutputReference().getName())));
	}
	
	private Collection<IResource> getResourcesFor(IResource resource, String targetKind) {
		return getResourcesFor(Arrays.asList(resource), targetKind);
	}
	
	private Collection<IResource> getResourcesFor(Collection<? extends IResource> resources, String targetKind) {
		Collection<IResource> set = new HashSet<>();
		for (IResource resource : resources) {
			Collection<IResource> targets = relationMap.get(getKey(resource, targetKind));
			if(targets != null) {
				set.addAll(targets);
			}
		}
		return set;
	}
	
	private <T extends IResource> List<T> load(String kind){
		List<T> resources = conn.getResources(kind, project.getName());
		resources.forEach(r->cache.put(getCacheKey(r), r));
		projectAdapter.setResources(new HashSet<>(resources), kind);
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
		if(start == null || end == null) return;
		String startKey = getKey(start, end.getKind());
		if(!relationMap.containsKey(startKey)) {
			relationMap.put(startKey, Collections.synchronizedSet(new HashSet<>()));
		}
		relationMap.get(startKey).add(end);
		String endKey = getKey(end, start.getKind());
		if(!relationMap.containsKey(endKey)) {
			relationMap.put(endKey, Collections.synchronizedSet(new HashSet<>()));
		}
		relationMap.get(endKey).add(start);
	}

	private Collection<IPod> getPodsForService(IService service, List<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return pods.stream()
			.filter(p->selectorsOverlap(serviceSelector, p.getLabels()))
			.collect(Collectors.toSet());
	}

	private void mapResourceToDeployment(IResource resource, Deployment deployment) {
		if(!resourceToDeployments.containsKey(resource)) {
			resourceToDeployments.put(resource, Collections.synchronizedList(new ArrayList<>()));
		}
		resourceToDeployments.get(resource).add(deployment);
	}
	
	public boolean isLoaded() {
		return State.LOADED == state.get();
	}
	
	public Collection<Deployment> getDeployments(){
		buildDeployments();
		return Collections.unmodifiableCollection(deployments);
	}

	private void mapServicesToRoutes(Collection<IService> services, Collection<IRoute> routes){
		Map<String, IRoute> serviceToRoute = routes.stream().collect(Collectors.toMap(IRoute::getServiceName, Function.identity()));
		services.forEach(s->createRelation(s, serviceToRoute.get(s.getName())));
	}
	
	private void mapServicesToRepControllers(Collection<IService> services, Collection<IReplicationController> rcs){
		for (IReplicationController rc : rcs) {
			Map<String, String> deploymentSelector = rc.getReplicaSelector();
			for (IService service : services) {
				Map<String, String> serviceSelector = service.getSelector();
				if(selectorsOverlap(serviceSelector, deploymentSelector)) {
					createRelation(service, rc);
				}
			}		
		}
	}
	
	private Collection<IBuild> getBuildsForPods(Collection<IPod> pods, Collection<IBuild> builds) {
		Collection<IBuild> buildsForDeployment = new HashSet<IBuild>();
		List<IDeploymentConfig> dcs = pods.stream()
			.map(p->getResourcesFor(p, ResourceKind.DEPLOYMENT_CONFIG))
			.flatMap(l->l.stream())
			.map(r->(IDeploymentConfig)r)
			.collect(Collectors.toList());
		Map<String, Collection<IDeploymentConfig>> imageRefToDeployConfigs = mapImageRefToDeployConfigs(dcs);
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
			projectAdapter.add(resource);
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
			projectAdapter.remove(resource);
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
			projectAdapter.update(resource);
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
