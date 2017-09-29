/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.util;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.OpenShiftResourceSelectors;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.image.IImageStreamImport;
import com.openshift.restclient.model.route.IRoute;

public class ResourceUtils {

	public static final String DOCKER_IMAGE_KIND = "DockerImage";
	public static final String IMAGE_STREAM_IMAGE_KIND = "ImageStreamImage";
	public static final String DEPLOYMENT_CONFIG = "deploymentconfig";

	public static IClient getClient(IResource resource) {
		return resource.accept(new CapabilityVisitor<IClientCapability, IClient>() {
			@Override
			public IClient visit(IClientCapability cap) {
				return cap.getClient();
			}
		}, null);
	}

	public static IClient checkedGetClient(IResource resource) throws CoreException {
		IClient client = getClient(resource);
		if (client == null) {
			throw new CoreException(
					StatusFactory.errorStatus(OpenShiftCoreActivator.PLUGIN_ID, 
							NLS.bind("Could not get client for resource {0}", resource.getName())));
		}
		return client;
	}

	
	/**
	 * Returns {@code true} if the given route points to the given service and
	 * the given service is the service that the given route points to.
	 * 
	 * @param route
	 * @param service
	 * @return
	 * 
	 * @see IRoute#getServiceName()
	 * @see IService#getName()
	 */
	public static boolean areRelated(IRoute route, IService service) {
		if (service != null 
				&& !StringUtils.isEmpty(service.getName())
				&& route != null) {
			return service.getName().equals(route.getServiceName());
		}
		return false;
	}

	/**
	 * Returns {@code true} if the given build config matches the name of the
	 * given service.
	 * 
	 * @param config
	 * @param service
	 * @return
	 */
	public static boolean areRelated(final IBuildConfig config, final IService service) {
		if (service != null 
				&& !StringUtils.isEmpty(service.getName())
				&& config != null) {
			return service.getName().equals(config.getName());
		}
		return false;
	}

	public static boolean areRelated(IDeploymentConfig dc, IService s) {
		return containsAll(s.getSelector(), dc.getReplicaSelector());
	}

	public static boolean areRelated(IReplicationController rc, IService s) {
		return containsAll(s.getSelector(), rc.getReplicaSelector());
	}

	public static boolean areRelated(IReplicationController rc, IDeploymentConfig dc) {
		if (dc == null
				|| rc == null) {
			return false;
		}
		return StringUtils.equals(dc.getName(), getDeploymentConfigNameFor(rc));
	}

	public static boolean areRelated(IPod pod, IService s) {
		return containsAll(s.getSelector(), pod.getLabels());
	}

    public static boolean areRelated(IPod pod, IReplicationController rc) {
        return containsAll(rc.getReplicaSelector(), pod.getLabels());
    }

    /**
	 * Returns <code>true</code> if the given resource contains the given text
	 * in name or tags.
	 * 
	 * @param filterText
	 * @param resource
	 * @return
	 */
	public static boolean isMatching(final String filterText, IResource resource) {
		if(resource == null || StringUtils.isBlank(filterText)) {
			return true;
		}

		return resource.accept(new CapabilityVisitor<ITags, Boolean>() {
			@Override
			public Boolean visit(ITags capability) {
				return isMatching(filterText, resource.getName(), capability.getTags());
			}
		}, Boolean.FALSE);
	}

	public static boolean isMatching(final String filterText, String name, Collection<String> tags) {
		if (StringUtils.isBlank(filterText)) {
			return true;
		}
		final Set<String> items = new HashSet<>(Arrays.asList(
				filterText.replaceAll(",", " ").toLowerCase().split(" ")));
		if (containsAll(name, items)) {
			return true;
		}
		for (String item : items) {
			if (!inCollection(item, tags)) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsAll(String text, final Collection<String> items) {
		final String _text = text.toLowerCase();
		return items.stream()
				.allMatch(it -> {
					return _text.contains(it);
				});
	}

	private static boolean inCollection(String item, final Collection<String> texts) {
		final String _item = item.toLowerCase();
		return texts.stream().anyMatch((String txt)->{return txt.toLowerCase().contains(_item);});
	}

	/**
	 * Determine if the source map overlaps the target map (i.e. Matching a service to a pod). There
	 * is  match if the target includes all the the keys from the source and those keys have
	 * matching values
	 * 
	 * @param source
	 * @param target
	 * @return true if there is overlap; false; otherwise
	 */
	public static boolean containsAll(Map<String, String> source, Map<String, String> target) {
		if(source == null 
				|| target == null) {
			return false;
		}
		if(!target.keySet().isEmpty() 
				&& source.keySet().isEmpty()) {
			return false;
		}
		if(!target.keySet().containsAll(source.keySet())) {
			return false;
		}
		for (String key : source.keySet()) {
			if (!Objects.deepEquals(target.get(key), source.get(key))) {
				return false;
			}
		}
		return true;
	}
	
	public static Collection<IService> getServicesFor(IResource resource, Collection<IService> services) {
		if (resource instanceof IService) {
				return Collections.singleton((IService) resource);
		} else if (resource instanceof IReplicationController) {
			return getServicesFor((IReplicationController) resource, services);
		} else if (resource instanceof IPod) {
			return getServicesFor((IPod) resource, services);
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Find the collection of services whos selectors match the given pod
	 * @param pod
	 * @param services
	 * @return
	 */
	public static Collection<IService> getServicesFor(IPod pod, Collection<IService> services){
		return services.stream()
				.filter(s->containsAll(s.getSelector(), pod.getLabels()))
				.collect(Collectors.toSet());
	}
	
	public static Collection<IService> getServicesFor(IReplicationController rc, Collection<IService> services){
		return services.stream()
				.filter(service -> areRelated(rc, service))
				.collect(Collectors.toSet());
	}

	/**
	 * Find the collection of pods that match the selector of the given service
	 * @param service
	 * @param pods
	 * @return
	 */
	public static List<IPod> getPodsFor(IService service, List<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return getPodsForSelector(serviceSelector, pods);
	}

    /**
     * Find the collection of pods that match the deployment name annotation
     * @param replicationController the replication controller to match
     * @param pods the list of pods to search for
     * @return the matched pods
     */
    public static List<IPod> getPodsFor(IDeploymentConfig deploymentConfig, List<IPod> pods) {
        return pods
        		.stream()
			.filter(pod -> {
		            String configName = pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
		            return deploymentConfig.getName().equals(configName);
			})
			.collect(Collectors.toList());
    }

    /**
     * Find the collection of pods that match the selector of the given resource
     * @param resource the OpenShift resource to start from
     * @param pods the list of pods to search
     * @return the list of linked pods
     */
    public static List<IPod> getPodsFor(IResource resource, List<IPod> pods) {
        if (resource instanceof IService) {
            return getPodsFor((IService) resource, pods);
        } else if (resource instanceof IDeploymentConfig) {
            return getPodsFor((IDeploymentConfig)resource, pods);
        } else if (resource instanceof IReplicationController) {
            return getPodsFor((IReplicationController) resource, pods);
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Return the deployment config associated with this pod. Uses
     * annotations to do the matching.
     * 
     * @param pod the pod to look for
     * @return the deployment config
     */
    public static Optional<IDeploymentConfig> getDeploymentConfig(IPod pod, Collection<IDeploymentConfig> deploymentConfigs) {
        return deploymentConfigs.stream()
        .filter(dc -> dc.getName().equals(pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME)))
        .findFirst();
    }
    
    /**
     * Return the deployment config or replication controller associated with this pod. Uses
     * annotations to do the matching.
     * 
     * @param pod the pod to look for
     * @return the deployment config or replication controller
     */
    public static IReplicationController getDeploymentConfigOrReplicationControllerFor(IPod pod) {
        Optional<? extends IResource> rcOrDc = getDeploymentConfig(pod, pod.getProject().getResources(ResourceKind.DEPLOYMENT_CONFIG));
        if (!rcOrDc.isPresent()) {
            rcOrDc = Optional.ofNullable(getReplicationControllerFor(pod, pod.getProject().getResources(ResourceKind.REPLICATION_CONTROLLER)));
        }
        return (IReplicationController) rcOrDc.orElse(null);
    }

    /**
	 * Find the collection of pods that match the given selector
	 * @param selector
	 * @param pods
	 * @return
	 */
	private static List<IPod> getPodsForSelector(Map<String, String> serviceSelector, Collection<IPod> pods) {
		return pods.stream()
				.filter(p -> containsAll(serviceSelector, p.getLabels()))
				.distinct()
				.collect(Collectors.toList());
	}
	
	/**
	 * Returns {@code true} if the given pod is a pod running builds. This is
	 * the case if the pod is annotated with the build name. Returns
	 * {@code false} if the given pod is a pod running an application or is
	 * null.
	 * 
	 * @param pod
	 *            the pod that shall be checked whether it's a build pod
	 * @return true if pod is annotated with the build name; false otherwise;
	 * 
	 * @see IPod
	 * @see OpenShiftAPIAnnotations#BUILD_NAME
	 */
	public static boolean isBuildPod(IPod pod) {
		if (pod == null) {
			return false;
		}
		return pod.isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME);
	}

	public static Collection<IPod> getBuildPods(Collection<IPod> allPods) {
		return allPods.stream()
				.filter(pod -> isBuildPod(pod))
				.collect(Collectors.toList());
	}
	
	/**
	 * Returns {@code true} if the given pod is a pod running deployments. This is
	 * the case if the pod has a label which tells it to be a deployer for some pod. Returns
	 * {@code false} otherwise.
	 * 
	 * @param pod
	 *            the pod that shall be checked whether it's a deployer pod
	 * @return true if pod is annotated with the build name; false otherwise;
	 * 
	 * @see IPod
	 * @see OpenShiftAPIAnnotations#DEPLOYER_POD_FOR
	 */
	public static boolean isDeployerPod(IPod pod) {
		if (pod == null) {
			return false;
		}
		return pod.getLabels().containsKey(OpenShiftAPIAnnotations.DEPLOYER_POD_FOR);
	}

	/**
	 * Returns {@code true} if the given pod is a pod running applications. This is
	 * the case if the pod is neither a build nor deployer pod. Returns
	 * {@code false} otherwise.
	 * example:
	 * <pre>
	 *  "labels": {
     *       "openshift.io/deployer-pod-for.name": "nodejs-5"
     *   },
	 * </pre>
	 * 
	 * @param pod
	 *            the pod that shall be checked whether it's a runtime/application pod
	 * @return true if pod is annotated with the build name; false otherwise;
	 * 
	 * @see IPod
	 * @see OpenShiftAPIAnnotations#DEPLOYER_POD_FOR
	 */
	public static boolean isRuntimePod(IPod pod) {
		if (pod == null) {
			return false;
		}
		return !isBuildPod(pod)
				&& !isDeployerPod(pod);
	}

	public static Collection<IPod> getRuntimePods(Collection<IPod> allPods) {
		return allPods.stream()
				.filter(pod -> isRuntimePod(pod))
				.collect(Collectors.toList());
	}

	/**
	 * The image reference for an image change trigger used to correlate a 
	 * deploymentconfig to a buildconfig
	 *  
	 * @param trigger
	 * @return
	 */
	public static String imageRef(IDeploymentImageChangeTrigger trigger) {
		if(trigger != null) {
			switch(trigger.getKind()) {
			case ResourceKind.IMAGE_STREAM_TAG:
			case IMAGE_STREAM_IMAGE_KIND:
			case DOCKER_IMAGE_KIND:	
				return trigger.getFrom().getNameAndTag();
			}
		}
		return "";
	}
	
	/**
	 * Returns all the images for the given build configs.
	 * @param buildConfigs the build configs to extract the image refs from
	 * @return all the image references within the given build configs
	 * 
	 * @see #imageRef(IBuildConfig)
	 */
	public static List<String> getImageRefs(List<IBuildConfig> buildConfigs) {
		if (buildConfigs == null) {
			return null;
		}
		return buildConfigs.stream()
				.map(bc -> imageRef(bc))
				.collect(Collectors.toList());
	}
	
	/**
	 * The image reference for an image change trigger used to correlate a 
	 * buildconfig to a deploymentconfig
	 *  
	 * @param trigger
	 * @return
	 */
	public static String imageRef(IBuildConfig config) {
		if (config != null) {
			IObjectReference outputRef = config.getBuildOutputReference();
			if (outputRef != null) {
				String kind = outputRef.getKind();
				if (ResourceKind.IMAGE_STREAM_TAG.equals(kind) 
						|| IMAGE_STREAM_IMAGE_KIND.equals(kind)) {
					return outputRef.getName();
				}
			}
		}
		return "";
	}

	/**
	 * Returns the image referenced by a given build. Returns empty string if none is found.
	 *  
	 * @param build
	 * @return the image referenced
	 */
	public static String imageRef(IBuild build) {
		if (build != null) {
			switch(build.getOutputKind()) {
				case ResourceKind.IMAGE_STREAM_TAG:
				case IMAGE_STREAM_IMAGE_KIND:
				case DOCKER_IMAGE_KIND:
					return build.getOutputTo().getNameAndTag();
			}
		}
		return "";
	}

	/**
	 * Returns the image stream tag among the given ones that matches the given docker
	 * image digest. The image stream tag property in "image.metadata.name" is being
	 * matched against the given digest.
	 * 
	 * @param digest the digest that the image stream tag shall match in the property "image.metadata.name"
	 * @param imageStreamTags the image stream tags to inspect
	 * @return
	 */
	public static IResource getImageStreamTagForDigest(String digest, Collection<? extends IResource> imageStreamTags) {
		return imageStreamTags.stream()
				.filter(istag -> {
					String imageName = ModelNode.fromJSONString(istag.toJson()).get("image").get("metadata").get("name").asString();
					return digest.equals(imageName);
				})
				.findFirst().orElse(null);
	}
	
	/**
	 * Find the collection of pods for the given replication controller
	 * @param replicationController the replication controller to search pods for
	 * @param pods the list of pods to search
	 * @return the list of matched pods
	 */
	public static List<IPod> getPodsFor(IReplicationController replicationController, List<IPod> allPods) {
		Map<String, String> selector = replicationController.getReplicaSelector();
		return getPodsForSelector(selector, allPods);
	}

	/**
	 * Returns the first route that's found and matches the given service.
	 * 
	 * @param service
	 * @param routes
	 * @return
	 */
	public static IRoute getRouteFor(final IService service, Collection<IRoute> routes) {
		List<IRoute> matchingRoutes = getRoutesFor(service, routes);
		if (matchingRoutes.isEmpty()) {
			return null;
		} else {
			return matchingRoutes.get(0);
		}
	}

	/**
	 * 
	 * Returns the routes from the given routes that match the given service.
	 * 
	 * @param service
	 * @param routes
	 * @return
	 */
	public static List<IRoute> getRoutesFor(final IService service, Collection<IRoute> routes) {
		if (routes == null
				|| routes.isEmpty()) {
			return Collections.emptyList();
		}
		return routes.stream()
				.filter(r -> areRelated(r, service))
				.collect(Collectors.toList());
	}

	/**
	 * Returns build configs of the given list of build configs
	 * that match the given service.
	 * 
	 * @param service
	 * @param buildConfigs
	 * @return
	 * 
	 * @see #areRelated(IBuildConfig, IService)
	 * @see IBuildConfig
	 * @see IService
	 */
	public static List<IBuildConfig> getBuildConfigsFor(IService service, List<IBuildConfig> buildConfigs) {
		if (buildConfigs == null
				|| buildConfigs.isEmpty()) {
			return Collections.emptyList();
		}

		return buildConfigs.stream()
				.filter(bc -> areRelated(bc, service))
				.collect(Collectors.toList());
	}

    /**
	 * Returns the 1st replication controllers that's found matching the given
	 * service. The lookup is done by matching the label in the service and
	 * replication controller pod template. No existing pods are required.
	 * 
	 * @param service
	 * @param allReplicationControllers
	 * @return
	 */
	public static IReplicationController getReplicationControllerFor(IService service, List<IReplicationController> allReplicationControllers) {
		if (allReplicationControllers == null
				|| allReplicationControllers.isEmpty()
				|| service == null) {
			return null;
		}
		List<IReplicationController> rcs = allReplicationControllers.stream()
				.filter(rc -> containsAll(service.getSelector(), rc.getTemplateLabels()))
				.collect(Collectors.toList());
		return getLatestDeploymentConfigVersion(rcs);
	}

    /**
     * Returns the 1st replication controllers that's found matching the given
     * service. The lookup is done by matching the label in the service and
     * replication controller pod template. No existing pods are required.
     * 
     * @param pod
     * @param allReplicationControllers
     * @return
     */
    public static IReplicationController getReplicationControllerFor(IPod pod, List<IReplicationController> allReplicationControllers) {
        if (allReplicationControllers == null
                || allReplicationControllers.isEmpty()
                || pod == null) {
            return null;
        }

        return allReplicationControllers.stream()
                .filter(rc -> containsAll(rc.getReplicaSelector(), pod.getLabels()))
                .findFirst()
                .orElse(null);
    }

	public static IReplicationController getLatestDeploymentConfigVersion(List<IReplicationController> rcs) {
		if (rcs == null 
				|| rcs.isEmpty()) {
			return null;
		}

		return rcs.stream()
				.max(new NumericResourceAttributeComparator<IReplicationController>() {

					@Override
					protected int getResourceAttribute(IReplicationController rc) {
						return safeParseInt(rc.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_LATEST_VERSION));
					}
				})
				.orElse(null);
	}

	/**
	 * Returns {@code true} if the given 1st resource has a lower resource version
	 * as the given 2nd resource. Failing to retrieve the resource version on any of
	 * the 2 resources also returns {@code true}.
	 * 
	 * @param thisResource
	 * @param thatResource
	 * @return true if the 1st resource is older than the 2nd one
	 * 
	 * @see
	 */
	public static boolean isOlder(IResource thisResource, IResource thatResource) {
		try {
			int thisVersion = Integer.parseInt(thisResource.getResourceVersion());
			int thatVersion = Integer.parseInt(thatResource.getResourceVersion());
			return thisVersion < thatVersion;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	/**
	 * Returns the latest version of the given collection of deployment configs.
	 * 
	 * @param resources
	 * @return
	 */
	public static <R extends IResource> R getLatestResourceVersion(List<R> resources) {
		if (resources == null 
				|| resources.isEmpty()) {
			return null;
		}

		return resources.stream()
				.max(new NumericResourceAttributeComparator<R>() {

					@Override
					protected int getResourceAttribute(R resource) {
						return safeParseInt(resource.getResourceVersion());
					}
				})
				.orElse(null);
	}

	private abstract static class NumericResourceAttributeComparator<R> implements Comparator<R>{

		@Override
		public int compare(R r1, R r2) {
			if (r1 == null) {
				if (r2 == null) {
					return 0;
				} else {
					return 1;
				}
			} else {
				if (r2 == null) {
					return -1;
				} else {
					int attr1 = getResourceAttribute(r1);
					int attr2 = getResourceAttribute(r2);
					if (attr1 < attr2) {
						return -1;
					} else if (attr1 == attr2) {
						return 0;
					} else {
						return 1;
					}
				}
			}
		}

		protected abstract int getResourceAttribute(R r); 

		protected int safeParseInt(String string) {
			try {
				return Integer.parseInt(string);
			} catch(NumberFormatException e1) {
				return -1;
			}
		}
	}

	/**
	 * Returns the first build config out of the given list of build configs
	 * that matches the given service.
	 * 
	 * @param service
	 *            the service that the build configs shall match
	 * @param buildConfigs
	 *            the build configs that shall be introspected
	 * @return
	 * 
	 * @see #getBuildConfigsFor(IService, List)
	 * @see #areRelated(IBuildConfig, IService)
	 * @see IBuildConfig
	 * @see IService
	 */
	public static IBuildConfig getBuildConfigFor(IService service, List<IBuildConfig> buildConfigs) {
		List<IBuildConfig> matchinBuildConfigs = getBuildConfigsFor(service, buildConfigs);
		if (matchinBuildConfigs.isEmpty()) {
			return null;
		} else {
			return matchinBuildConfigs.get(0);
		}
	}

    /**
     * Returns the first build config out of the given list of build configs
     * that matches the given deployment config.
     * 
     * @param deploymentConfig
     *            the deployment config that the build configs shall match
     * @param buildConfigs
     *            the build configs that shall be introspected
     * @return
     * 
     * @see #getBuildConfigsForService(IService, List)
     * @see #areRelated(IBuildConfig, IService)
     * @see IBuildConfig
     * @see IDeploymentConfig
     */
    private static IBuildConfig getBuildConfigFor(IDeploymentConfig deploymentConfig, List<IBuildConfig> buildConfigs) {
        List<IBuildConfig> matchinBuildConfigs = getBuildConfigsFor(deploymentConfig, buildConfigs);
        if (matchinBuildConfigs.isEmpty()) {
            return null;
        } else {
            return matchinBuildConfigs.get(0);
        }
    }

    /**
     * Returns build configs of the given list of build configs
     * that match the given deployment config.
     * 
     * @param serv
     * @param buildConfigs
     * @return
     * 
     * @see #areRelated(IBuildConfig, IService)
     * @see IBuildConfig
     * @see IService
     */
    public static List<IBuildConfig> getBuildConfigsFor(IDeploymentConfig deploymentConfig, List<IBuildConfig> buildConfigs) {
        if (buildConfigs == null
                || buildConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        return buildConfigs.stream()
                .filter(bc -> areRelated(bc, deploymentConfig))
                .collect(Collectors.toList());
    }

    /**
     * Returns the first build config out of the given list of build configs
     * that matches the given OpenShift resource (service, replication controller,...).
     * 
     * @param resource
     *            the OpenShift resource that the build configs shall match
     * @param buildConfigs
     *            the build configs that shall be introspected
     * @return
     * 
     * @see #getBuildConfigsForService(IService, List)
     * @see IBuildConfig
     */
	public static IBuildConfig getBuildConfigFor(IResource resource, List<IBuildConfig> buildConfigs) {
		if (resource == null) {
			return null;
		}

		if (ResourceKind.SERVICE.equals(resource.getKind())) {
	        return getBuildConfigFor((IService) resource, buildConfigs);
	    } else if (ResourceKind.DEPLOYMENT_CONFIG.equals(resource.getKind())) {
	        return getBuildConfigFor((IDeploymentConfig) resource, buildConfigs);
	    } else {
	        return null;
	    }
	}

    /**
     * Returns {@code true} if the given build config matches the name of the
     * given service.
     * 
     * @param config
     * @param deploymentConfig
     * @return
     */
    public static boolean areRelated(final IBuildConfig config, final IDeploymentConfig deploymentConfig) {
        if (deploymentConfig != null 
                && !StringUtils.isEmpty(deploymentConfig.getName())
                && config != null) {
            return deploymentConfig.getName().equals(config.getName());
        }
        return false;
    }

    /**
	 * Returns {@code true} if the given service and given deployment config are related given the existing pods.
	 * @param service the service to match
	 * @param dc the deployment config to match
	 * @return true if they are related
	 */
    public static boolean areRelated(final IService service, IDeploymentConfig dc, Collection<IPod> allPods) {
		if (dc == null) {
			return false;
		}
		String dcName = dc.getName();
		return allPods.stream()
				.filter(pod -> dcName.equals(pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME)))
				.filter(pod -> areRelated(pod, service))
				.count() > 0;
    }

	/**
	 * Returns git controlled workspace projects that match the uri of the given build config.
	 *   
	 * @param buildConfig the build config whose source git shall be matched
	 * @param workspaceProjects all workspace projects that shall be inspected
	 * @return
	 * 
	 * @see IBuildConfig#getSourceURI()
	 * @see org.eclipse.core.resources.IProject
	 * @see EGitUtils#isSharedWithGit(org.eclipse.core.resources.IProject)
	 */
	public static org.eclipse.core.resources.IProject getWorkspaceProjectFor(
			IBuildConfig buildConfig, List<org.eclipse.core.resources.IProject> workspaceProjects) {
		if (workspaceProjects == null
				|| workspaceProjects.isEmpty()) {
			return null;
		}

		return workspaceProjects.stream()
			// only git shared projects
			.filter(project -> EGitUtils.isSharedWithGit(project))
			.filter(project -> {
					try {
						if (buildConfig != null 
								&& !StringUtils.isEmpty(buildConfig.getSourceURI())) {
							return EGitUtils.getAllRemoteURIs(project)
									.contains(new URIish(buildConfig.getSourceURI()));
						}
					} catch (CoreException | URISyntaxException e) {
					}
					return false;
			})
			.findFirst().orElseGet(() -> null);
	}

	public static boolean isSuccessful(IImageStreamImport imageStreamImport) {
		return imageStreamImport.getImageStatus().stream()
				.filter(s -> s.isSuccess()).findFirst().isPresent();
	}

	public static String getDeploymentConfigNameFor(List<IPod> pods) {
		if (pods == null
				|| pods.isEmpty()) {
			return null;
		}

		return pods.stream()
				.filter(pod -> 
					!StringUtils.isBlank(getSelectorMatchingAny(pod.getLabels(), OpenShiftResourceSelectors.DEPLOYMENT_CONFIG)))
				.findFirst()
				.map(pod -> getDeploymentConfigNameFor(pod))
				.orElse(null);
	}
	
	public static IProject getProject(IResource resource) {
		IProject project = null;
		if (resource instanceof IProject) {
			project = (IProject) resource;
		} else if (resource != null) {
			project = resource.getProject();
		}
		return project;
	}

	/**
	 * Returns the name of the deployment config that created the given resource.
	 * The name is taken from the annotation for the given resource. Returns
	 * {@code null} if no reference was found within the annotation of the given
	 * resource.
	 * 
	 * @param rc
	 * @return
	 */
	private static String getDeploymentConfigNameFor(IReplicationController rc) {
		return rc.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
		/**
		 * alternatively, the deployment config name is also in the replicaSelectors
		 * with the key OpenShiftResourceSelectors#DEPLOYMENT_CONFIG
		 * 
		 * example:
		 * 
		 * <pre>
		 * "spec": {
		 *     "replicas": 1,
		 *     "selector": {
		 *         "deployment": "eap-app-2",
		 *         "deploymentConfig": "eap-app",
		 *         "deploymentconfig": "eap-app",
		 * </pre>
		 */
	}

	private static String getDeploymentConfigNameFor(IService svc) {
		return getDeploymentConfigNameFor(svc.getSelector());
	}

	private static String getDeploymentConfigNameFor(Map<String, String> selector) {
		if (MapUtils.isEmpty(selector)) {
			return null;
		}
		return getSelectorMatchingAny(selector,
				OpenShiftResourceSelectors.DEPLOYMENT_CONFIG);
	}

	/**
	 * Returns the deployment config for the given resource and given connection.
	 * Returns {@code null} otherwise. Resource types supported are
	 * <ul>
	 * <li>IDeploymentConfig</li>
	 * <li>IService</li>
	 * <li>IReplicationController</li>
	 * </ul>
	 * Should <strong>NOT</strong> be called from UI thread since it does remote
	 * lookups to list and match existing deployment configs.
	 * 
	 * @param resource the resource to get the deployment config for
	 * @param connection the connection to use for further resource queries on server
	 * @return
	 */
	public static IDeploymentConfig getDeploymentConfigFor(IResource resource, Connection connection) {
		IDeploymentConfig dc = null;
		if (resource instanceof IDeploymentConfig) {
			dc = (IDeploymentConfig) resource;
		} else if (resource instanceof IService) {
			dc = getDeploymentConfigFor((IService) resource, connection);
		} else if (resource instanceof IReplicationController) {
			dc = getDeploymentConfigFor((IReplicationController) resource, connection);
		}
		return dc;
	}

	private static IDeploymentConfig getDeploymentConfigFor(IService service, Connection connection) {
		if (service == null) {
			return null;
		}
		String dcName = getDeploymentConfigNameFor(service);
		if (dcName != null) {
			return getDeploymentConfigByName(dcName, service, connection);
		} else {
			String namespace = service.getNamespace();
			IReplicationController rc = getReplicationControllerFor(
					service, connection.getResources(ResourceKind.REPLICATION_CONTROLLER, namespace));
			if (rc == null) {
				return null;
			}
			List<IPod> allPods = connection.getResources(ResourceKind.POD, namespace);
			List<IPod> pods = allPods.stream()
				.filter(pod -> areRelated((IPod) pod, rc))
				.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(pods)) {
				return null;
			}
			List<IDeploymentConfig> dcs = connection.getResources(ResourceKind.DEPLOYMENT_CONFIG, namespace);
			return dcs.stream()
				.filter(dc -> areRelated((IService) service, (IDeploymentConfig) dc, pods))
				.findFirst()
				.orElse(null);
		}
	}

	public static IDeploymentConfig getDeploymentConfigFor(IReplicationController rc, Connection connection) {
		String dcName = getDeploymentConfigNameFor(rc);
		if (dcName != null) {
			return getDeploymentConfigByName(dcName, rc, connection);
		} else {
			List<IDeploymentConfig> allDcs = connection.getResources(ResourceKind.DEPLOYMENT_CONFIG, rc.getNamespace());
			return allDcs.stream()
					.filter(dc -> ResourceUtils.areRelated(rc, dc))
					// TODO: what if several dc are found?
					.findFirst().orElse(null);
		}
	}
	
	private static IDeploymentConfig getDeploymentConfigByName(String dcName, IResource resource, Connection connection) {
		if (StringUtils.isBlank(dcName) 
				|| resource == null) {
			return null;
		}
		return connection.getResource(ResourceKind.DEPLOYMENT_CONFIG, resource.getNamespace(), dcName);
	}
	
	/**
	 * Returns the value of the selector that matches one of the matching keys.
	 * Returns {@code null} otherwise.
	 * 
	 * @param selectors
	 * @param keys
	 * @return
	 */
	private static String getSelectorMatchingAny(Map<String, String> selectors, String... keys) {
		if (keys == null
				|| keys.length == 0) {
			return null;
		}
		Optional<String> matchingSelector = selectors.keySet().stream()
				.filter(selector -> Stream.of(keys)
						.anyMatch(key -> selector.equalsIgnoreCase(key)))
				.findFirst();
		if (!matchingSelector.isPresent()) {
			return null;
		}
		return selectors.get(matchingSelector.get());
	}

	private static String getDeploymentConfigNameFor(IPod pod) {
		return getSelectorMatchingAny(pod.getLabels(), OpenShiftResourceSelectors.DEPLOYMENT_CONFIG);
	}

	/**
	 * Returns the deployment config from the given collection of configs for the given replication controller.
	 * Returns {@code null} if the given replication controller is {@code null}.
	 * 
	 * @param r the replication controller that is related to the requested deployment config
	 * @param dcs all the deployments configs to choose from
	 * @return
	 */
	public static IDeploymentConfig getDeploymentConfigFor(IReplicationController rc, Collection<IDeploymentConfig> dcs) {
		if (rc == null) {
			return null;
		}

		return getDeploymentConfigByName(getDeploymentConfigNameFor(rc), dcs);
	}

	/**
	 * Returns the latest version of the given collection of deployment configs
	 * whose name is matching the given name.
	 * Returns {@code null} if the given name or the collection
	 * 
	 * @param dcs
	 * @param name
	 * @return
	 */
	private static IDeploymentConfig getDeploymentConfigByName(String name, Collection<IDeploymentConfig> dcs) {
		if (name == null 
				|| CollectionUtils.isEmpty(dcs)) {
			return null;
		}

		return dcs.stream()
			.filter(dc -> name.equals(dc.getName()))
			.max(new NumericResourceAttributeComparator<IDeploymentConfig>() {

				@Override
				protected int getResourceAttribute(IDeploymentConfig dc) {
					return safeParseInt(dc.getResourceVersion());
				}
			})
			.orElse(null);
	}

	/**
	 * Extracts the last segment of an URI, stripped from .git suffixes
	 *
	 * Made public for testing purposes.
	 */
	public static String getProjectNameForURI(String uri) {
		String projectName = null;
		if (StringUtils.isNotEmpty(uri)) {
			String mangledUri = uri.trim();
			mangledUri = stripTrailingSlashes(mangledUri);
			mangledUri = stripDotGit(mangledUri);
			if (mangledUri != null) {
				projectName = getLastPathSegment(mangledUri);
			}
		}
		return projectName;
	}

	private static String stripDotGit(String uri) {
		String strippedUri = uri;
		if (strippedUri.endsWith(".git")) {
			strippedUri = strippedUri.substring(0, strippedUri.length() - 4);
			if (strippedUri.endsWith("/")) {
				// '/' before .git is error
				return null;
			}
		}
		return strippedUri;
	}

	private static String getLastPathSegment(String mangledUri) {
		int index = mangledUri.lastIndexOf('/');
		if (index >= 0) {
			return mangledUri.substring(index + 1);
		} else {
			return mangledUri;
		}
	}

	/**
	 * Removes (multiple) trailing "/" from the given uri
	 * 
	 * @param uri
	 * @return
	 */
	private static String stripTrailingSlashes(String uri) {
		String strippedUri = uri;
		while (strippedUri.endsWith("/")) {
			// Trailing slashes do not matter.
			strippedUri = strippedUri.substring(0, strippedUri.length() - 1);
		}
		return strippedUri;
	}
}
