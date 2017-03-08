/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
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
import java.util.stream.Collectors;

import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.route.IRoute;

/**
 * This class encapsulates the logic used for determinining which resource in a
 * project are related to whilch service.
 * 
 * @author thomas
 * @author Andre Dietisheim
 * @author Jeff Maury
 *
 */
public class ServiceResourceMapper {
	public static final Collection<IResource> computeRelatedResources(IService s, Collection<IResource> resources) {
		Collection<IResource> result = new HashSet<>();

		resources.forEach(resource -> {
			if (resource instanceof IPod) {
				if (ResourceUtils.areRelated((IPod) resource, s)) {
					result.add(resource);
					result.addAll(getRelated(resources, (IPod) resource));
				}
			} else if (resource instanceof IDeploymentConfig) {
				if (ResourceUtils.areRelated((IDeploymentConfig) resource, s)) {
					result.add(resource);
					result.addAll(getRelated(resources, (IDeploymentConfig) resource));
				}
			} else if (resource instanceof IRoute) {
				if (ResourceUtils.areRelated((IRoute)resource, s)) {
					result.add(resource);
				}
			}
		});
		return result;
	}

    public static final Collection<IResource> computeRelatedResources(IDeploymentConfig dc, Collection<IResource> resources) {
        return getRelated(resources, dc);
    }
    
    public static final Collection<IResource> computeRelatedResources(IReplicationController rc, Collection<IResource> resources) {
        Collection<IResource> pods = new ArrayList<>();
        pods.addAll(getRelatedPods(resources, rc));
        return pods;
    }

    private static Collection<IResource> getRelated(Collection<IResource> resources, IPod resource) {
		return getRelatedReplicationControllers(resources, Collections.singleton(resource));
	}

	private static Collection<IResource> getRelated(Collection<IResource> resources, IDeploymentConfig dc) {
		Collection<IResource> result = new HashSet<>();

		Collection<String> dcImageRefs = computeImageRefs(dc);

		result.addAll(getRelatedImageTags(resources, dcImageRefs, dc));
		Collection<IBuildConfig> buildConfigs = getRelatedBuildConfigs(resources, dcImageRefs, dc);
		result.addAll(buildConfigs);
		Collection<IBuild> builds = getRelatedBuilds(resources, dcImageRefs, buildConfigs);
		result.addAll(builds);
		Collection<IPod> pods = getRelatedPods(resources, builds, dc);
		result.addAll(pods);
		result.addAll(getRelatedReplicationControllers(resources, pods));
		result.addAll(getRelatedReplicationControllers(resources, dc));
		return result;
	}

	private static Collection<IResource> getRelatedReplicationControllers(Collection<IResource> resources, IDeploymentConfig dc) {
		return resources.stream().filter(r-> {
			 return dc.getName().equals(r.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME));
		}).collect(Collectors.toList());
	}

	private static Collection<IResource> getRelatedReplicationControllers(Collection<IResource> resources,
			Collection<IPod> pods) {
		Collection<String> deploymentNames = pods.stream()
				.filter(r -> r.isAnnotatedWith(OpenShiftAPIAnnotations.DEPLOYMENT_NAME))
				.map(r -> r.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_NAME)).collect(Collectors.toSet());

		Collection<IResource> result = new HashSet<>();
		resources.forEach(r -> {
			if (ResourceKind.REPLICATION_CONTROLLER.equals(r.getKind()) && deploymentNames.contains(r.getName())) {
				result.add(r);
			}
		});
		return result;
	}

	private static Collection<IPod> getRelatedPods(Collection<IResource> resources, Collection<IBuild> builds,
			IDeploymentConfig dc) {

		Collection<String> buildNames = builds.stream().map(bc -> bc.getName()).collect(Collectors.toSet());

		return resources.stream().filter(r -> {
			return r instanceof IPod && (buildNames.contains(r.getAnnotation(OpenShiftAPIAnnotations.BUILD_NAME))
					|| dc.getName().equals(r.getAnnotation(OpenShiftAPIAnnotations.DEPLOYMENT_CONFIG_NAME)));
		}).map(r -> (IPod) r).collect(Collectors.toSet());

	}

    private static Collection<IPod> getRelatedPods(Collection<IResource> resources, IReplicationController rc) {

        return resources.stream().filter(r -> {
            return r instanceof IPod && ResourceUtils.areRelated((IPod) r, rc);
        }).map(r -> (IPod) r).collect(Collectors.toSet());

    }

    private static Collection<IBuild> getRelatedBuilds(Collection<IResource> resources, Collection<String> dcImageRefs,
			Collection<IBuildConfig> buildConfigs) {

		Collection<IBuild> result = new HashSet<>();
		Collection<String> bcNames = buildConfigs.stream().map(bc -> bc.getName()).collect(Collectors.toSet());
		resources.forEach(r -> {
			if (r instanceof IBuild) {
				IBuild build = (IBuild) r;
				if (bcNames.contains(r.getLabels().get(OpenShiftAPIAnnotations.BUILD_CONFIG_NAME)) || dcImageRefs.contains(imageRef(build))) {
					result.add((IBuild) r);
				}
			}
		});
		return result;
	}

	private static Collection<IBuildConfig> getRelatedBuildConfigs(Collection<IResource> resources,
			Collection<String> dcImageRefs, IDeploymentConfig dc) {
		Collection<IBuildConfig> result = new HashSet<>();
		resources.forEach(r -> {
			if (r instanceof IBuildConfig && dcImageRefs.contains(imageRef((IBuildConfig) r))) {
				result.add((IBuildConfig) r);
			}
		});
		return result;
	}

	private static Collection<IResource> getRelatedImageTags(Collection<IResource> resources,
			Collection<String> dcImageRefs, IDeploymentConfig dc) {
		Collection<IResource> result = new HashSet<>();
		resources.forEach(r -> {
			if (ResourceKind.IMAGE_STREAM_TAG.equals(r.getKind()) && dcImageRefs.contains(r.getName())) {
				result.add(r);
			}
		});

		return result;
	}

	public static Collection<String> computeImageRefs(IDeploymentConfig dc) {
		Collection<String> imageRefs = new HashSet<>(dc.getTriggers().size());
		dc.getTriggers().forEach(trigger -> {
			if (DeploymentTriggerType.IMAGE_CHANGE.equals(trigger.getType())) {
				imageRefs.add(imageRef((IDeploymentImageChangeTrigger) trigger));
			}
		});
		return imageRefs;
	}

    public static Collection<IResource> getServices(IReplicationController rc, Collection<IResource> resources) {
        return resources.stream().filter(r -> ResourceKind.SERVICE.equals(r.getKind()))
                          .filter(s -> ResourceUtils.areRelated(rc, (IService) s))
                          .collect(Collectors.toList());
    }

}
