/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.openshift3.client.model.IBuildConfig;
import com.openshift3.client.model.build.ICustomBuildStrategy;
import com.openshift3.client.model.build.IDockerBuildStrategy;
import com.openshift3.client.model.build.ISTIBuildStrategy;

public class BuildConfigPropertySource extends DefaultResourcePropertySource {
	
	private static final String IMAGE = "Image";
	private static final String ENVIRONMENT_VARIABLES = "Environment Variables";
	private static final String STRATEGY = "Strategy";
	private IBuildConfig config;
	
	public BuildConfigPropertySource(IBuildConfig resource) {
		super(resource);
		this.config = resource;
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		switch(config.getBuildStrategy().getType()){
		case Custom:
			return getCustomPropertyDescriptors();
		case Docker:
			return getDockerPropertyDescriptors();
		case STI:
			return getSTIPropertyDescriptors();
		default:
		}
		return super.getResourcePropertyDescriptors();
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(id instanceof Ids){
			switch((Ids)id){
			case Type: return config.getBuildStrategy().getType();
			case CUSTOM_EXPOSE_DOCKER_SOCKET:
				return config.<ICustomBuildStrategy>getBuildStrategy().exposeDockerSocket();
			case CUSTOM_ENV:
				return new KeyValuePropertySource(config.<ICustomBuildStrategy>getBuildStrategy().getEnvironmentVariables());
			case CUSTOM_IMAGE:
				return config.<ICustomBuildStrategy>getBuildStrategy().getImage();
			case DOCKER_CONTEXT_DIR:
				return config.<IDockerBuildStrategy>getBuildStrategy().getContextDir();
			case DOCKER_IMAGE:
				return config.<IDockerBuildStrategy>getBuildStrategy().getBaseImage();
			case STI_SCRIPT_LOCATION: 
				return config.<ISTIBuildStrategy>getBuildStrategy().getScriptsLocation();
			case STI_IMAGE:
				return config.<ISTIBuildStrategy>getBuildStrategy().getImage();
			case STI_ENV:
				return new KeyValuePropertySource(config.<ISTIBuildStrategy>getBuildStrategy().getEnvironmentVariables());
			default:
			}
		}
		return super.getPropertyValue(id);
	}

	private IPropertyDescriptor[] getDockerPropertyDescriptors() {
		return new  IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.DOCKER_CONTEXT_DIR, "Expose Docker Socket", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.DOCKER_IMAGE, IMAGE, STRATEGY),
		};
	}
	
	
	private IPropertyDescriptor[] getCustomPropertyDescriptors() {
		return new  IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_EXPOSE_DOCKER_SOCKET, "Expose Docker Socket", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_IMAGE, IMAGE, STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_ENV, ENVIRONMENT_VARIABLES, STRATEGY),
		};
	}
	
	private IPropertyDescriptor[]  getSTIPropertyDescriptors(){
		return new  IPropertyDescriptor[]{
			new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_SCRIPT_LOCATION, "Script Location", STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_IMAGE, IMAGE, STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_ENV, ENVIRONMENT_VARIABLES, STRATEGY),
		};
	}
	public static enum Ids{
		Type,
		CUSTOM_EXPOSE_DOCKER_SOCKET, 
		CUSTOM_IMAGE, 
		CUSTOM_ENV,
		DOCKER_CONTEXT_DIR, 
		DOCKER_IMAGE,
		STI_SCRIPT_LOCATION,
		STI_IMAGE,
		STI_ENV
	}
}
