/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.IBuildTrigger;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.route.IRoute;

/**
 * Simple content provider for handling a list of resources
 * 
 * @author jeff.cantrill
 */
public class ResourceDetailsContentProvider implements ITreeContentProvider{

	@Override
	public Object[] getChildren(Object node) {
		if(node instanceof IResource) {
			IResource resource = (IResource) node;
			Collection<ResourceProperty> properties = new ArrayList<ResourceProperty>();
			properties.add(new ResourceProperty("labels", resource.getLabels()));
			switch(resource.getKind()) {
			case BuildConfig:
				getBuildConfigChildren(properties, (IBuildConfig) resource); 
				break;
			case DeploymentConfig:
				getDeploymentConfigChildren(properties, (IDeploymentConfig) resource); 
				break;
			case Service:
				getServiceChildren(properties, (IService) resource); 
				break;
			case Route:
				getRouteChildren(properties, (IRoute) resource); 
				break;
			case ImageStream:
				getImageStreamChildren(properties, (IImageStream) resource); 
				break;
			default:
			}
			return properties.toArray();
		}
		return new Object[] {};
	}
	
	private void getRouteChildren(Collection<ResourceProperty> properties, IRoute resource) {
		properties.add(new ResourceProperty("host", resource.getHost()));
		properties.add(new ResourceProperty("path", resource.getPath()));
		properties.add(new ResourceProperty("service", resource.getServiceName()));
	}

	private void getImageStreamChildren(Collection<ResourceProperty> properties, IImageStream resource) {
		properties.add(new ResourceProperty("registry", resource.getDockerImageRepository()));
	}

	private void getServiceChildren(Collection<ResourceProperty> properties, IService resource) {
		properties.add(new ResourceProperty("selector", resource.getSelector()));
		properties.add(new ResourceProperty("port", resource.getPort()));
	}

	private void getDeploymentConfigChildren(Collection<ResourceProperty> properties, IDeploymentConfig resource) {
		properties.add(new ResourceProperty("triggers", resource.getTriggerTypes()));
		properties.add(new ResourceProperty("strategy", resource.getDeploymentStrategyType()));
		properties.add(new ResourceProperty("template selector", resource.getReplicaSelector()));
	}

	private void getBuildConfigChildren(Collection<ResourceProperty> properties, IBuildConfig config) {
		IBuildStrategy buildStrategy = config.getBuildStrategy();
		properties.add(new ResourceProperty("strategy", buildStrategy.getType().toString()));
		switch(buildStrategy.getType()) {
		case STI:
			ISTIBuildStrategy sti = (ISTIBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("builder image", sti.getImage().toString()));
			break;
		case Docker:
			IDockerBuildStrategy docker = (IDockerBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("base image", docker.getBaseImage().toString()));
			break;
		case Custom:
			ICustomBuildStrategy custom = (ICustomBuildStrategy) buildStrategy;
			properties.add(new ResourceProperty("builder image", custom.getImage().toString()));
			break;
		default:
		}
		properties.add(new ResourceProperty("source URL", config.getSourceURI()));
		properties.add(new ResourceProperty("output to", config.getOutputRepositoryName()));
		Collection<String> triggers = new ArrayList<String>();
		for (IBuildTrigger trigger : config.getBuildTriggers()) {
			triggers.add(trigger.getType().toString());
		}
		properties.add(new ResourceProperty("build triggers", triggers));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object[] getElements(Object rootElements) {
		if(!(rootElements instanceof Collection)) return new Object[] {};
		List<IResource> resources = new ArrayList<IResource>( (Collection<IResource>)rootElements);
		Collections.sort(resources, new Comparator<IResource>() {
			@Override
			public int compare(IResource first, IResource second) {
				int result = first.getKind().toString().compareTo(second.getKind().toString());
				if(result !=0) return result;
				return first.getName().compareTo(second.getName());
			}
		});
		return resources.toArray();
	}

	@Override
	public boolean hasChildren(Object node) {
		return node instanceof IResource;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldValue, Object newValue) {
	}

	@Override
	public Object getParent(Object paramObject) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * A wrapper for a resource properties
	 */
	public static class ResourceProperty {
		
		private Object value;
		private String property;
		/**
		 * @param property
		 * @param value
		 */
		ResourceProperty(String property, Object value){
			this.property = property;
			this.value = value;
		}
		
		String getProperty() {
			return property;
		}
		
		Object getValue() {
			return value;
		}

	}
}
