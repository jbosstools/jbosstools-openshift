/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.build.BuildStrategyType;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.IJenkinsPipelineStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;
import com.openshift.restclient.model.route.IRoute;

/**
 * Simple content provider for handling a list of resources
 * 
 * @author jeff.cantrill
 */
public class ResourceDetailsContentProvider implements ITreeContentProvider{

	public static final String LABEL_STRATEGY = "strategy";

	@Override
	public Object[] getChildren(Object node) {
		if(node instanceof IResource) {
			IResource resource = (IResource) node;
			Collection<ResourceProperty> properties = new ArrayList<>();
			properties.add(new ResourceProperty("labels", resource.getLabels()));
			switch(resource.getKind()) {
			case ResourceKind.BUILD_CONFIG:
				addBuildConfigProperties(properties, (IBuildConfig) resource); 
				break;
			case ResourceKind.DEPLOYMENT_CONFIG:
				addDeploymentConfigProperties(properties, (IDeploymentConfig) resource); 
				break;
			case ResourceKind.SERVICE:
				addServiceProperties(properties, (IService) resource); 
				break;
			case ResourceKind.ROUTE:
				addRouteProperties(properties, (IRoute) resource); 
				break;
			case ResourceKind.IMAGE_STREAM:
				addImageStreamProperties(properties, (IImageStream) resource); 
				break;
			default:
			}
			return properties.toArray();
		}
		return new Object[] {};
	}
	
	private void addRouteProperties(Collection<ResourceProperty> properties, IRoute resource) {
		properties.add(new ResourceProperty("host", resource.getHost()));
		properties.add(new ResourceProperty("path", resource.getPath()));
		properties.add(new ResourceProperty("service", resource.getServiceName()));
	}

	private void addImageStreamProperties(Collection<ResourceProperty> properties, IImageStream resource) {
		properties.add(new ResourceProperty("registry", resource.getDockerImageRepository()));
	}

	private void addServiceProperties(Collection<ResourceProperty> properties, IService resource) {
		properties.add(new ResourceProperty("selector", resource.getSelector()));
		properties.add(new ResourceProperty("port", resource.getPort()));
	}

	private void addDeploymentConfigProperties(Collection<ResourceProperty> properties, IDeploymentConfig resource) {
		properties.add(new ResourceProperty("triggers", resource.getTriggerTypes()));
		properties.add(new ResourceProperty(LABEL_STRATEGY, resource.getDeploymentStrategyType()));
		properties.add(new ResourceProperty("template selector", resource.getReplicaSelector()));
	}

	private void addBuildConfigProperties(Collection<ResourceProperty> properties, IBuildConfig config) {
		IBuildStrategy buildStrategy = config.getBuildStrategy();
		addStrategyTypeProperties(properties, buildStrategy);
		properties.add(new ResourceProperty("source URL", config.getSourceURI()));
		properties.add(new ResourceProperty("output to", config.getOutputRepositoryName()));
		List<String> triggers = config.getBuildTriggers().stream()
				.map(trigger -> trigger.getType().toString())
				.collect(Collectors.toList());
		properties.add(new ResourceProperty("build triggers", triggers));
	}

	private void addStrategyTypeProperties(Collection<ResourceProperty> properties, IBuildStrategy buildStrategy) {
		if (buildStrategy == null
				|| buildStrategy.getType() == null) {
			properties.add(new UnknownResourceProperty(LABEL_STRATEGY));
			return;
		}

		properties.add(new ResourceProperty(LABEL_STRATEGY, buildStrategy.getType().toString()));
		switch(buildStrategy.getType()) {
		case BuildStrategyType.SOURCE:
			ISourceBuildStrategy sti = (ISourceBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("builder image", StringUtils.toStringOrNull(sti.getImage())));
			break;
		case BuildStrategyType.DOCKER:
			IDockerBuildStrategy docker = (IDockerBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("base image", StringUtils.toStringOrNull(docker.getBaseImage())));
			break;
		case BuildStrategyType.CUSTOM:
			ICustomBuildStrategy custom = (ICustomBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("builder image", StringUtils.toStringOrNull(custom.getImage())));
			break;
		case BuildStrategyType.JENKINS_PIPELINE:
			IJenkinsPipelineStrategy jenkins = (IJenkinsPipelineStrategy) buildStrategy;
			properties.add(new ResourceProperty("jenkins file", StringUtils.removeAll(StringUtils.getLineSeparator(), jenkins.getJenkinsfile())));
			properties.add(new ResourceProperty("jenkins file path", StringUtils.toStringOrNull(jenkins.getJenkinsfilePath())));
			break;
		default:
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getElements(Object rootElements) {
		if(!(rootElements instanceof Collection)) {
			return new Object[] {};
		}
	
		List<IResource> resources = new ArrayList<>((Collection<IResource>) rootElements);
		Collections.sort(resources, new ResourceKindAndNameComparator());
		return resources.toArray();
	}

	@Override
	public boolean hasChildren(Object node) {
		return node instanceof IResource;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldValue, Object newValue) {
	}

	@Override
	public Object getParent(Object paramObject) {
		return null;
	}

	/**
	 * A wrapper for a resource properties
	 */
	public static class ResourceProperty {
		
		private Object value;
		private String property;

		ResourceProperty(String property, Object value){
			this.property = property;
			this.value = value;
		}
		
		public String getProperty() {
			return property;
		}
		
		public Object getValue() {
			return value;
		}

		public boolean isUnknownValue() {
			return false;
		}
		
	}

	public static class UnknownResourceProperty extends ResourceProperty {

		UnknownResourceProperty(String property) {
			super(property, null);
		}

		@Override
		public boolean isUnknownValue() {
			return true;
		}
	}
	
	private static class ResourceKindAndNameComparator implements Comparator<IResource> {
		@Override
		public int compare(IResource first, IResource second) {
			int result = compareKind(first, second);
			if (result != 0) {
				return result;
			}
			return compareName(first, second);
		}

		private int compareName(IResource first, IResource second) {
			return first.getName().compareTo(second.getName());
		}

		private int compareKind(IResource first, IResource second) {
			int result = first.getKind().toString().compareTo(second.getKind().toString());
			return result;
		}
	}
}
