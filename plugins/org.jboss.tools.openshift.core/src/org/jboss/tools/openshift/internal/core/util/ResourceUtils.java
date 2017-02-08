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
package org.jboss.tools.openshift.internal.core.util;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.egit.core.EGitUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
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
	public static final String DEPLOYMENT_CONFIG_KEY = "deploymentconfig";

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
	
	/**
	 * Find the collection of services whos selectors match the given pod
	 * @param pod
	 * @param services
	 * @return
	 */
	public static Collection<IService> getServicesForPod(IPod pod, Collection<IService> services){
		return services.stream()
				.filter(s->containsAll(s.getSelector(), pod.getLabels()))
				.collect(Collectors.toSet());
	}
	
	/**
	 * Find the collection of pods that match the selector of the given service
	 * @param service
	 * @param pods
	 * @return
	 */
	public static List<IPod> getPodsForService(IService service, Collection<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return getPodsForSelector(serviceSelector, pods);
	}

    /**
     * Find the collection of pods that match the deployment name annotation
     * @param replicationController the replication controller to match
     * @param pods the list of pods to search for
     * @return the matched pods
     */
    public static List<IPod> getPodsForReplicationController(IReplicationController replicationController, Collection<IPod> pods) {
        return pods.stream().filter(pod -> {
            String configName = pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_NAME);
            return replicationController.getName().equals(configName);
        }).collect(Collectors.toList());
    }

    /**
     * Find the collection of pods that match the deployment name annotation
     * @param replicationController the replication controller to match
     * @param pods the list of pods to search for
     * @return the matched pods
     */
    public static List<IPod> getPodsForDeployementConfig(IDeploymentConfig deploymentConfig, Collection<IPod> pods) {
        return pods.stream().filter(pod -> {
            String configName = pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME);
            return deploymentConfig.getName().equals(configName);
        }).collect(Collectors.toList());
    }

    /**
     * Find the collection of pods that match the selector of the given resource
     * @param resource the OpenShift resource to start from
     * @param pods the list of pods to search
     * @return the list of linked pods
     */
    public static List<IPod> getPodsForResource(IResource resource, Collection<IPod> pods) {
        if (resource instanceof IService) {
            return getPodsForService((IService) resource, pods);
        } else if (resource instanceof IDeploymentConfig) {
            return getPodsForDeployementConfig((IDeploymentConfig)resource, pods);
        } else if (resource instanceof IReplicationController) {
            return getPodsForReplicationController((IReplicationController) resource, pods);
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Return the deployment config or replication controller associated with this pod. Uses
     * annotations to do the matching.
     * 
     * @param pod the pod to look for
     * @return the deployment config or replication controller
     */
    public static IReplicationController getDeploymentConfigOrReplicationControllerForPod(IPod pod) {
        Optional<IResource> rcOrDc = pod.getProject().getResources(ResourceKind.DEPLOYMENT_CONFIG).stream()
            .filter(dc -> dc.getName().equals(pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME)))
            .findFirst();
        if (!rcOrDc.isPresent()) {
            rcOrDc = pod.getProject().getResources(ResourceKind.REPLICATION_CONTROLLER).stream()
                    .filter(dc -> dc.getName().equals(pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_NAME)))
                    .findFirst();
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
	 * 
	 * @param pod
	 * @return true if pod is annotated with the build name; false otherwise;
	 */
	public static boolean isBuildPod(IPod pod) {
		return pod.isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME);
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
	 * The image reference for an image change trigger used to correlate a 
	 * build to a deploymentconfig
	 *  
	 * @param trigger
	 * @return
	 */
	public static String imageRef(IBuild build) {
		if(build != null) {
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
	 * Find the collection of pods for the given deployment config
	 * @param deploymentConfig
	 * @param pods
	 * @return
	 */
	public static Collection<IPod> getPodsForDeploymentConfig(IDeploymentConfig deploymentConfig) {
		List<IPod> pods = deploymentConfig.getProject().getResources(ResourceKind.POD);
		Map<String, String> selector = deploymentConfig.getReplicaSelector();
		return getPodsForSelector(selector, pods);
	}

	/**
	 * Returns the first route that's found and matches the given service.
	 * 
	 * @param service
	 * @param routes
	 * @return
	 */
	public static IRoute getRouteForService(final IService service, Collection<IRoute> routes) {
		List<IRoute> matchingRoutes = getRoutesForService(service, routes);
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
	public static List<IRoute> getRoutesForService(final IService service, Collection<IRoute> routes) {
		if (routes == null
				|| routes.isEmpty()) {
			return Collections.emptyList();
		}
		return routes.stream()
				.filter(r -> areRelated(r, service))
				.collect(Collectors.toList());
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
	public static List<IBuildConfig> getBuildConfigsForService(IService service, List<IBuildConfig> buildConfigs) {
		if (buildConfigs == null
				|| buildConfigs.isEmpty()) {
			return Collections.emptyList();
		}

		return buildConfigs.stream()
				.filter(bc -> areRelated(bc, service))
				.collect(Collectors.toList());
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
    public static List<IBuildConfig> getBuildConfigsForDeploymentConfig(IDeploymentConfig deploymentConfig, List<IBuildConfig> buildConfigs) {
        if (buildConfigs == null
                || buildConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        return buildConfigs.stream()
                .filter(bc -> areRelated(bc, deploymentConfig))
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
	public static IReplicationController getReplicationControllerForService(IService service, List<IReplicationController> allReplicationControllers) {
		if (allReplicationControllers == null
				|| allReplicationControllers.isEmpty()
				|| service == null) {
			return null;
		}

		return allReplicationControllers.stream()
				.filter(rc -> containsAll(service.getSelector(), rc.getTemplateLabels()))
				.findFirst()
				.orElse(null);
	}

	public static IReplicationController selectByDeploymentConfigVersion(List<IReplicationController> rcs) {
		if (rcs == null 
				|| rcs.isEmpty()) {
			return null;
		}
		
		return rcs.stream()
				.sorted((IReplicationController rc1, IReplicationController rc2) -> {
					if (rc1 == null) {
						if (rc2 == null) {
							return 0;
						} else {
							return 1;
						}
					} else {
						if (rc2 == null) {
							return -1;
						} else {
							return compareVersions(
									rc1.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_LATEST_VERSION), 
									rc2.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_LATEST_VERSION));
						}
					}
				})
				.findFirst()
				.orElse(null);
	}
	
	private static int compareVersions(String version1, String version2) {
		int v1 = safeParseInt(version1);
		int v2 = safeParseInt(version2);
		if (v1 > v2) {
			return -1;
		} else if (v1 == v2) {
			return 0;
		} else {
			return 1;
		}
	}

	private static int safeParseInt(String string) {
		try {
			return Integer.parseInt(string);
		} catch(NumberFormatException e1) {
			return -1;
		}
	}
	
    /**
     * Returns the first build config out of the given list of build configs
     * that matches the given service.
     * 
     * @param resource
     *            the service that the build configs shall match
     * @param buildConfigs
     *            the build configs that shall be introspected
     * @return
     * 
     * @see #getBuildConfigsForService(IService, List)
     * @see #areRelated(IBuildConfig, IService)
     * @see IBuildConfig
     * @see IService
     */
    public static IBuildConfig getBuildConfigForService(IService service, List<IBuildConfig> buildConfigs) {
        List<IBuildConfig> matchinBuildConfigs = getBuildConfigsForService(service, buildConfigs);
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
    private static IBuildConfig getBuildConfigForDeploymentConfig(IDeploymentConfig deploymentConfig, List<IBuildConfig> buildConfigs) {
        List<IBuildConfig> matchinBuildConfigs = getBuildConfigsForDeploymentConfig(deploymentConfig, buildConfigs);
        if (matchinBuildConfigs.isEmpty()) {
            return null;
        } else {
            return matchinBuildConfigs.get(0);
        }
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
	public static IBuildConfig getBuildConfigForResource(IResource resource, List<IBuildConfig> buildConfigs) {
	    if (ResourceKind.SERVICE.equals(resource.getKind())) {
	        return getBuildConfigForService((IService) resource, buildConfigs);
	    } else if (ResourceKind.DEPLOYMENT_CONFIG.equals(resource.getKind())) {
	        return getBuildConfigForDeploymentConfig((IDeploymentConfig) resource, buildConfigs);
	    } else {
	        return null;
	    }
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
	 * Checks whether the service and deployment config are related.
	 * @param service the service to match
	 * @param dc the deployment config to match
	 * @return true if they are related
	 */
    public static boolean areRelated(final IService service, IDeploymentConfig dc) {
        return service.getProject().getResources(ResourceKind.POD).stream()
                                                                   .filter(pod -> containsAll(service.getSelector(), pod.getLabels()))   
                                                                   .filter(pod -> dc.getName().equals(pod.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME)))   
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
	public static org.eclipse.core.resources.IProject getWorkspaceProjectForBuildConfig(
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
				}
	
				)
			.findFirst().orElseGet(() -> null);
	}

	public static boolean isSuccessful(IImageStreamImport imageStreamImport) {
		return imageStreamImport.getImageStatus().stream()
				.filter(s -> s.isSuccess()).findFirst().isPresent();
	}

	public static String getDeploymentConfigNameForPods(List<IPod> pods) {
		if (pods == null
				|| pods.isEmpty()) {
			return null;
		}

		return pods.stream()
				.filter(pod -> pod.getLabels().containsKey(DEPLOYMENT_CONFIG_KEY))
				.findFirst()
				.map(pod -> pod.getLabels().get(DEPLOYMENT_CONFIG_KEY))
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

}
