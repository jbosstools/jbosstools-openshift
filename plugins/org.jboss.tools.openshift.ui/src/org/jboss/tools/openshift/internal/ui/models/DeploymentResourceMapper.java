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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
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
 * Figures out the resources in a project associated with a deployment
 * 
 * @author jeff.cantrill
 *
 */
public class DeploymentResourceMapper extends ObservablePojo implements OpenShiftAPIAnnotations, IRefreshable {

	private static enum State {
		UNINITIALIZED, LOADING, LOADED
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
	private IConnectionsRegistryListener connectionListener = new ConnectionListener();

	static {
		RELATIONSHIP_TYPE_MAP.put(ResourceKind.BUILD,
				new String[] { ResourceKind.BUILD_CONFIG, ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.POD });
		RELATIONSHIP_TYPE_MAP.put(ResourceKind.SERVICE, new String[] { ResourceKind.ROUTE, ResourceKind.POD });
	}

	public DeploymentResourceMapper(Connection conn, IProjectAdapter projectAdapter) {
		this.projectAdapter = projectAdapter;
		this.project = projectAdapter.getProject();
		this.conn = conn;
		WatchManager.getInstance().startWatch(project);
	}

	@Override
	public synchronized void refresh() {
		synchronized (deployments) {
			deployments.clear();
		}
			
		for (@SuppressWarnings("rawtypes")
		Map map : new Map[] { resourceToDeployments, imageRefToDeployConfigs, relationMap, cache }) {
			map.clear();// need to fire events?
		}
		state.set(State.UNINITIALIZED);
		buildDeployments();
	}

	private void buildDeployments() {
		if (state.compareAndSet(State.UNINITIALIZED, State.LOADING)) {
			try {
				ConnectionsRegistrySingleton.getInstance().removeListener(connectionListener);

				List<IReplicationController> rcs = load(ResourceKind.REPLICATION_CONTROLLER);
				List<IService> services = load(ResourceKind.SERVICE);
				List<IRoute> routes = load(ResourceKind.ROUTE);
				List<IBuild> builds = load(ResourceKind.BUILD);
				List<IPod> pods = load(ResourceKind.POD);
				List<IDeploymentConfig> deployConfigs = load(ResourceKind.DEPLOYMENT_CONFIG);
				List<IBuildConfig> buildConfigs = load(ResourceKind.BUILD_CONFIG);
				List<IImageStream> imageStreams = load(ResourceKind.IMAGE_STREAM);

				imageRefToDeployConfigs = mapImageRefToDeployConfigs(deployConfigs);

				mapPods(pods, rcs, deployConfigs,builds);
				mapChildToParent(rcs, deployConfigs, DEPLOYMENT_CONFIG_NAME);
				mapChildToParent(builds, buildConfigs, BUILD_CONFIG_NAME, true);

				mapBuildConfigsToImageStreams(buildConfigs, imageStreams);
				mapBuildsToDeploymentConfigs(builds);
				mapServicesToRepControllers(services, rcs);
				mapServicesToRoutes(services, routes);

				services.forEach(service -> buildDeploymentFor(service, pods, builds));

				ConnectionsRegistrySingleton.getInstance().addListener(connectionListener);
			} finally {
				state.set(State.LOADED);
			}
		}
	}
	
	private void mapPods(Collection<IPod> pods, Collection<IReplicationController> rcs, Collection<IDeploymentConfig> deployConfigs, Collection<IBuild> builds) {
		mapChildToParent(pods, rcs, DEPLOYMENT_NAME);
		mapChildToParent(pods, deployConfigs, DEPLOYMENT_CONFIG_NAME);
		mapChildToParent(pods, builds, BUILD_NAME); // build pods
	}

	private void buildDeploymentFor(IService service, Collection<IPod> pods, Collection<IBuild> builds) {
		Deployment deployment = new Deployment(service, this.projectAdapter);
		Collection<IPod> appPods = getPodsForService(service, pods);
		Collection<IResource> appRcs = getResourcesFor(service, ResourceKind.REPLICATION_CONTROLLER);
		Collection<IBuild> appBuilds = getBuildsForPods(appPods, builds);
		Collection<IResource> appBuildConfigs = getResourcesFor(appBuilds, ResourceKind.BUILD_CONFIG);
		
		appPods.forEach(p->createRelation(p, service));
		
		deployment.setBuildResources(appBuilds);
		deployment.setPodResources(appPods);
		deployment.setRouteResources(getResourcesFor(service, ResourceKind.ROUTE));
		deployment.setBuildConfigResources(appBuildConfigs);
		deployment.setReplicationControllerResources(appRcs);
		deployment.setImageStreamResources(getResourcesFor(appBuildConfigs, ResourceKind.IMAGE_STREAM));
		deployment.setDeploymentConfigResources(getResourcesFor(appPods, ResourceKind.DEPLOYMENT_CONFIG));

		addDeployment(deployment);
	}

	public boolean isLoading() {
		return State.LOADING == state.get();
	}

	private synchronized void addDeployment(Deployment deployment) {
		mapResourcesFor(deployment);
		synchronized (deployments) {
			Collection<Deployment> old = new ArrayList<>(deployments);
			deployments.add(deployment);
			int index = deployments.size() - 1;
			fireIndexedPropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, index, old, new ArrayList<>(deployments));
		}
	}

	private void mapResourcesFor(Deployment deployment) {
		mapResourceToDeployment(deployment.getService(), deployment);
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
		buildConfigs.forEach(bc -> createRelation(bc, tagsToStreams.get(bc.getBuildOutputReference().getName())));
	}

	private Collection<IResource> getResourcesFor(IResource resource, String targetKind) {
		return getResourcesFor(Arrays.asList(resource), targetKind);
	}

	private Collection<IResource> getResourcesFor(Collection<? extends IResource> resources, String targetKind) {
		Collection<IResource> set = new HashSet<>();
		for (IResource resource : resources) {
			Collection<IResource> targets = relationMap.get(getKey(resource, targetKind));
			if (targets != null) {
				set.addAll(targets);
			}
		}
		return set;
	}

	private <T extends IResource> List<T> load(String kind) {
		List<T> resources = conn.getResources(kind, project.getName());
		projectAdapter.setResources(new HashSet<>(resources), kind);
		resources.forEach(r -> cache.put(getCacheKey(r), r));
		return resources;
	}

	private void mapBuildsToDeploymentConfigs(Collection<IBuild> builds) {
		builds.forEach(build -> mapBuildToDeploymentConfig(build));
	}

	private void mapBuildToDeploymentConfig(IBuild build) {
		String imageRef = imageRef(build, project);
		Collection<IDeploymentConfig> deploymentConfigs = imageRefToDeployConfigs.get(imageRef);
		if (deploymentConfigs != null) {
			deploymentConfigs.forEach(dc -> createRelation(build, dc));
		}
	}

	private void mapChildToParent(Collection<? extends IResource> manys, Collection<? extends IResource> ones,
			String annotation) {
		mapChildToParent(manys, ones, annotation, false);
	}

	/*
	 * Create a mapping between resources based on an annotation (e.g. pod->
	 * resourcecontroller). Mapping key is KIND::NAME
	 */
	private void mapChildToParent(Collection<? extends IResource> manys, Collection<? extends IResource> ones,
			String annotation, boolean annotationKeyIsLabel) {
		Map<String, IResource> parents = ones.stream()
				.collect(Collectors.toMap(IResource::getName, Function.identity()));
		for (IResource child : manys) {
			String parentName = annotationKeyIsLabel ? child.getLabels().get(annotation)
					: child.getAnnotation(annotation);
			if (parents.containsKey(parentName)) {
				IResource parent = parents.get(parentName);
				createRelation(child, parent);
			}
		}
	}

	/**
	 * Key for caching an object
	 * 
	 * @param resource
	 * @return
	 */
	private String getCacheKey(IResource resource) {
		return getCacheKey(resource.getName(), resource.getKind());
	}

	private String getCacheKey(String name, String kind) {
		return NLS.bind("{0}::{1}", name, kind);
	}

	/**
	 * The key for mapping relationships
	 * 
	 * @param resource
	 * @param targetKind
	 * @return
	 */
	private String getKey(IResource resource, String targetKind) {
		return getKey(resource.getName(), resource.getKind(), targetKind);
	}

	/**
	 * The key for mapping relationships
	 * 
	 * @param resource
	 * @param targetKind
	 * @return
	 */
	private String getKey(String name, String sourceKind, String targetKind) {
		return NLS.bind("{0}::{1}{2}{3}", new Object[] { name, sourceKind, RELATION_DELIMITER, targetKind });
	}

	private void createRelation(IResource start, IResource end) {
		if (start == null || end == null)
			return;
		String startKey = getKey(start, end.getKind());
		if (!relationMap.containsKey(startKey)) {
			relationMap.put(startKey, Collections.synchronizedSet(new HashSet<>()));
		}
		relationMap.get(startKey).add(end);
		String endKey = getKey(end, start.getKind());
		if (!relationMap.containsKey(endKey)) {
			relationMap.put(endKey, Collections.synchronizedSet(new HashSet<>()));
		}
		relationMap.get(endKey).add(start);
	}

	private Collection<IPod> getPodsForService(IService service, Collection<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return pods.stream().filter(p -> selectorsOverlap(serviceSelector, p.getLabels())).collect(Collectors.toSet());
	}
	
	private void mapResourceToDeployment(IResource resource, Deployment deployment) {
		if (!resourceToDeployments.containsKey(resource)) {
			resourceToDeployments.put(resource, Collections.synchronizedList(new ArrayList<>()));
		}
		resourceToDeployments.get(resource).add(deployment);
	}

	public boolean isLoaded() {
		return State.LOADED == state.get();
	}

	public Collection<Deployment> getDeployments() {
		buildDeployments();
		synchronized (deployments) {
			return new HashSet<>(deployments);
		}
	}
	
	private void mapServicesToRoutes(Collection<IService> services, Collection<IRoute> routes) {
		Map<String, IRoute> serviceToRoute = routes.stream()
				.collect(Collectors.toMap(IRoute::getServiceName, Function.identity()));
		services.forEach(s -> createRelation(s, serviceToRoute.get(s.getName())));
	}

	private void mapServicesToRepControllers(Collection<IService> services, Collection<IReplicationController> rcs) {
		for (IReplicationController rc : rcs) {
			Map<String, String> deploymentSelector = rc.getReplicaSelector();
			for (IService service : services) {
				Map<String, String> serviceSelector = service.getSelector();
				if (selectorsOverlap(serviceSelector, deploymentSelector)) {
					createRelation(service, rc);
				}
			}
		}
	}

	private Collection<IBuild> getBuildsForPods(Collection<IPod> pods, Collection<IBuild> builds) {
		Collection<IBuild> buildsForDeployment = new HashSet<IBuild>();
		List<IDeploymentConfig> dcs = pods.stream().map(p -> getResourcesFor(p, ResourceKind.DEPLOYMENT_CONFIG))
				.flatMap(l -> l.stream()).map(r -> (IDeploymentConfig) r).collect(Collectors.toList());
		Map<String, Collection<IDeploymentConfig>> imageRefToDeployConfigs = mapImageRefToDeployConfigs(dcs);
		for (IBuild build : builds) {
			String buildImageRef = imageRef(build, this.project);
			if (imageRefToDeployConfigs.containsKey(buildImageRef)) {
				buildsForDeployment.add(build);
			}
		}
		return buildsForDeployment;
	}

	private Map<String, Collection<IDeploymentConfig>> mapImageRefToDeployConfigs(
			Collection<IDeploymentConfig> configs) {
		Map<String, Collection<IDeploymentConfig>> map = new ConcurrentHashMap<>();
		for (IDeploymentConfig dc : configs) {
			List<IDeploymentTrigger> imageChangeTriggers = filterImageChangeTriggers(dc);
			for (IDeploymentTrigger trigger : imageChangeTriggers) {
				String imageRef = imageRef((IDeploymentImageChangeTrigger) trigger, this.project);
				if (!map.containsKey(imageRef)) {
					map.put(imageRef, Collections.synchronizedList(new ArrayList<>()));
				}
				map.get(imageRef).add(dc);
			}
		}
		return map;
	}

	private String imageRef(IBuild build, IProject project) {
		final String kind = build.getOutputKind();
		if (IMAGE_STREAM_TAG_KIND.equals(kind) || IMAGE_STREAM_IMAGE_KIND.equals(kind)) {
			return new DockerImageURI("", project.getName(), build.getOutputTo().getNameAndTag()).toString();
		}
		if (DOCKER_IMAGE_KIND.equals(kind)) {
			return build.getOutputTo().getNameAndTag().toString();
		}
		return "";

	}

	private String imageRef(IDeploymentImageChangeTrigger trigger, IProject project) {
		final String kind = trigger.getKind();
		if (IMAGE_STREAM_TAG_KIND.equals(kind) || IMAGE_STREAM_IMAGE_KIND.equals(kind)) {
			return new DockerImageURI("", project.getName(), trigger.getFrom().getNameAndTag()).toString();
		}
		if (DOCKER_IMAGE_KIND.equals(kind)) {
			return trigger.getFrom().getNameAndTag().toString();
		}
		return "";
	}

	private List<IDeploymentTrigger> filterImageChangeTriggers(IDeploymentConfig dc) {
		return dc.getTriggers().stream().filter(t -> t.getType().equals(DeploymentTriggerType.IMAGE_CHANGE))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private <T extends IResource> Collection<T> getResourcesOf(String kind) {
		return cache.values().stream().filter(r -> kind.equals(r.getKind())).map(r -> (T) r)
				.collect(Collectors.toList());
	}

	public synchronized void add(IResource resource) {
		try {
			Trace.debug("Trying to add resource to deployment {0}", resource);
			if(cache.containsKey(getCacheKey(resource))) {
				Trace.debug("-->Returning early since already processed {0}", resource);
			}
			cache.put(getCacheKey(resource), resource);
			projectAdapter.add(resource);
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				mapBuildToDeploymentConfig((IBuild) resource);
				break;
			case ResourceKind.REPLICATION_CONTROLLER:
				synchronized (cache) {
					Collection<IDeploymentConfig> deployConfigs = getResourcesOf(ResourceKind.DEPLOYMENT_CONFIG);
					mapChildToParent(Arrays.asList((IReplicationController)resource), deployConfigs, DEPLOYMENT_CONFIG_NAME);
				}
				break;
			case ResourceKind.SERVICE:
				synchronized (cache) {
					List<IService> services = Arrays.asList((IService) resource);
					mapServicesToRepControllers(services, getResourcesOf(ResourceKind.REPLICATION_CONTROLLER));
					mapServicesToRoutes(services, getResourcesOf(ResourceKind.ROUTE));
					Collection<IPod> pods = getResourcesOf(ResourceKind.POD);
					Collection<IBuild> builds = getResourcesOf(ResourceKind.BUILD);
					buildDeploymentFor(services.get(0), pods, builds);
				}
				break;
			case ResourceKind.POD:
				synchronized (cache) {
					IPod pod = (IPod) resource;
					List<IPod> pods = Arrays.asList(pod);
					Collection<IReplicationController> rcs = getResourcesOf(ResourceKind.REPLICATION_CONTROLLER);
					Collection<IDeploymentConfig> deployConfigs = getResourcesOf(ResourceKind.DEPLOYMENT_CONFIG);
					Collection<IBuild> builds = getResourcesOf(ResourceKind.BUILD);
					mapPods(pods, rcs, deployConfigs, builds);
					
					Map<String,String> podLabels = pod.getLabels();
					getResourcesOf(ResourceKind.SERVICE)
						.stream()
						.filter(s->selectorsOverlap(((IService)s).getSelector(), podLabels))
						.forEach(s->createRelation(s, pod));
					
				}
				break;
			}
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if (deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					deployment.add(resource);
					mapResourceToDeployment(resource, deployment);
				}
			}
			cache.put(getCacheKey(resource), resource);
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	public synchronized void remove(IResource resource) {
		try {
			Trace.debug("Trying to remove resource to deployment {0}", resource);
			if(!cache.containsKey(getCacheKey(resource))) {
				Trace.debug("-->Returning early since already processed {0}", resource);
			}
			cache.put(getCacheKey(resource), resource);
			projectAdapter.remove(resource);
			String resourceKind = resource.getKind();
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if (deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					resourceToDeployments.remove(resource);
					deployment.remove(resource);
				}
				if (ResourceKind.SERVICE.equals(resourceKind)) {
					deployments.forEach(d -> removeDeployment(d));
				}
			}
			if (RELATIONSHIP_TYPE_MAP.containsKey(resourceKind)) {
				for (String kind : RELATIONSHIP_TYPE_MAP.get(resourceKind)) {
					String left = getKey(resource, kind);
					if (relationMap.containsKey(left)) {
						for (IResource target : relationMap.get(left)) {
							String right = getKey(target, resourceKind);
							if (relationMap.containsKey(right)) {
								relationMap.get(right).remove(resource);
							}
						}
						relationMap.remove(left);
					}
				}
			}
			cache.remove(getCacheKey(resource));
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	private void removeDeployment(Deployment deployment) {
		synchronized (deployments) {
			final int index = deployments.indexOf(deployment);
			if (index > -1) {
				Collection<Deployment> old = new ArrayList<>(deployments);
				deployments.remove(index);
				fireIndexedPropertyChange(IProjectAdapter.PROP_DEPLOYMENTS, index, old, new ArrayList<>(deployments));
			}
		}
	}

	public synchronized void update(IResource resource) {
		try {
			Trace.debug("Trying to update resource for a deployment {0}", resource);
			if(alreadyProcessedResource(resource)) {
				Trace.debug("-->Returning early since already have this change: {0}", resource);
			}
			projectAdapter.update(resource);
			Collection<Deployment> deployments = findDeploymentsFor(resource);
			if (deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					deployment.update(resource);
				}
			}
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
	
	private boolean alreadyProcessedResource(IResource resource) {
		final String cacheKey = getCacheKey(resource);
		return cache.containsKey(cacheKey) && Integer.parseInt(cache.get(cacheKey).getResourceVersion()) >= Integer.parseInt(resource.getResourceVersion());
	}

	private Collection<Deployment> findDeploymentsFor(IResource resource) {
		if(resource != null) {
			Trace.debug("Looking for deployment associated with: {0}", resource);
			// build->dc->rc->pod->d
			Collection<Deployment> deployments = getDeploymentsFor(resource);
			if(!deployments.isEmpty()) {
				return deployments;
			}
			String path = null;
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				path = ResourceKind.DEPLOYMENT_CONFIG;
				break;
			case ResourceKind.DEPLOYMENT_CONFIG:
				path = ResourceKind.POD;
				break;
			case ResourceKind.REPLICATION_CONTROLLER:
				path = ResourceKind.DEPLOYMENT_CONFIG;
				break;
			case ResourceKind.POD:
				IPod pod = (IPod) resource;
				if(pod.isAnnotatedWith(BUILD_NAME)) {
					return getDeploymentsFor(cache.get(getCacheKey(pod.getAnnotation(BUILD_NAME), ResourceKind.BUILD)));
				}else if(pod.isAnnotatedWith(DEPLOYMENT_NAME)) {
					return getDeploymentsFor(cache.get(getCacheKey(pod.getAnnotation(DEPLOYMENT_NAME), ResourceKind.REPLICATION_CONTROLLER)));
				}
			default:
			}
			if (path != null) {
				String key = getKey(resource, path);
				if (relationMap.containsKey(key) && relationMap.get(key) != null) {
					deployments = new ArrayList<>();
					for (IResource relation : relationMap.get(key)) {
						deployments.addAll(findDeploymentsFor(relation));
					}
					return deployments;
				}
			}
		}
		return Collections.emptyList();
	}
	
	private Collection<Deployment> getDeploymentsFor(IResource resource){
		if (resourceToDeployments.containsKey(resource)) {
			return new HashSet<>(resourceToDeployments.get(resource));
		}
		return Collections.emptySet();
	}

	private class ConnectionListener extends ConnectionsRegistryAdapter {

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (!conn.equals(connection))
				return;
			if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
				if (oldValue == null && newValue != null) {
					// add
					handleChange((IResource) newValue, mapper->add(mapper));

				} else if (oldValue != null && newValue == null) {
					// delete
					handleChange((IResource) oldValue, mapper->remove(mapper));
				} else {
					// update
					handleChange((IResource) newValue, mapper->update(mapper));
				}
			}
		}
		
		private void handleChange(IResource resource, Consumer<IResource> action) {
			if (!project.getName().equals(resource.getNamespace())) {
				return;
			}
			action.accept(resource);
		}
	}

}
