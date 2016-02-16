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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;

public class ResourceUtils {

	public static final String DOCKER_IMAGE_KIND = "DockerImage";
	public static final String IMAGE_STREAM_IMAGE_KIND = "ImageStreamImage";

	/**
	 * Returns <code>true</code> if the given resource contains the given text
	 * in name or tags.
	 * 
	 * @param filterText
	 * @param template
	 * @return
	 */
	public static boolean isMatching(final String filterText, IResource template) {
		if (StringUtils.isBlank(filterText)) {
			return true;
		}

		final Set<String> items = new HashSet<>(Arrays.asList(filterText.replaceAll(",", " ").toLowerCase().split(" ")));
		if (containsAll(template.getName(), items)) {
			return true;
		}

		return template.accept(new CapabilityVisitor<ITags, Boolean>() {
			@Override
			public Boolean visit(ITags capability) {
				for (String item : items) {
					if (!inCollection(item, capability.getTags())) {
						return false;
					}
				}
				return true;
			}
		}, Boolean.FALSE);
	}

	private static boolean containsAll(String text, final Collection<String> items) {
		final String _text = text.toLowerCase();
		return items.stream().allMatch((String it)->{return _text.contains(it);});
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
		if(source == null || target == null) return false;
		if(!target.keySet().isEmpty() && source.keySet().isEmpty()) {
			return false;
		}
		if(!target.keySet().containsAll(source.keySet())) {
			return false;
		}
		for (String key : source.keySet()) {
			if(!Objects.deepEquals(target.get(key),source.get(key))) {
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
				.filter(s->containsAll(((IService)s).getSelector(), pod.getLabels()))
				.collect(Collectors.toSet());
	}
	
	/**
	 * Find the collection of pods that match the selector of the given service
	 * @param service
	 * @param pods
	 * @return
	 */
	public static Collection<IPod> getPodsForService(IService service, Collection<IPod> pods) {
		final Map<String, String> serviceSelector = service.getSelector();
		return getPodsForSelector(serviceSelector, pods);
	}

	/**
	 * Find the collection of pods that match the given selector
	 * @param selector
	 * @param pods
	 * @return
	 */
	public static Collection<IPod> getPodsForSelector(Map<String, String> serviceSelector, Collection<IPod> pods) {
		return pods.stream()
				.filter(p -> containsAll(serviceSelector, p.getLabels()))
				.collect(Collectors.toSet());
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
	 * The image reference for an image change trigger used to correlate a 
	 * buildconfig to a deploymentconfig
	 *  
	 * @param trigger
	 * @return
	 */
	public static String imageRef(IBuildConfig config) {
		if(config != null) {
			IObjectReference outputRef = config.getBuildOutputReference();
			switch(outputRef.getKind()) {
				case ResourceKind.IMAGE_STREAM_TAG:
				case IMAGE_STREAM_IMAGE_KIND:
					return outputRef.getName();
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
}
