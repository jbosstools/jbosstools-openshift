/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.build.ImageChangePropertySource;
import org.jboss.tools.openshift.internal.ui.property.build.WebHooksPropertySource;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildSourceType;
import com.openshift.restclient.model.build.BuildStrategyType;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.IGitBuildSource;
import com.openshift.restclient.model.build.ISTIBuildStrategy;

public class BuildConfigPropertySource extends ResourcePropertySource<IBuildConfig> {
	
	private static final String TRIGGERS = "Triggers";
	private static final String IMAGE = "Image";
	private static final String ENVIRONMENT_VARIABLES = "Environment Variables";
	private static final String STRATEGY = "Strategy";
	private static final String SOURCE = "Source";
	
	public BuildConfigPropertySource(IBuildConfig resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		List<IPropertyDescriptor> all = new ArrayList<IPropertyDescriptor>();
		all.addAll(getBuildTriggerPropertyDescriptors());
		switch(getResource().getBuildStrategy().getType()){
		case BuildStrategyType.CUSTOM:
			all.addAll(getCustomPropertyDescriptors());
			break;
		case BuildStrategyType.DOCKER:
			all.addAll(getDockerPropertyDescriptors());
			break;
		case BuildStrategyType.STI:
		case BuildStrategyType.SOURCE:
			all.addAll(getSTIPropertyDescriptors());
			break;
		default:
		}
		switch(getResource().getBuildSource().getType()){
		case BuildSourceType.GIT:
			all.addAll(getGitBuildSource());
			break;
		default:
		}
		all.add(new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.OUTPUT_REPO_NAME, "Image Stream Name", "Output"));
		return all.toArray(new IPropertyDescriptor[]{});
	}

	private List<IPropertyDescriptor> getBuildTriggerPropertyDescriptors() {
		return Arrays.asList(new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.TRIGGERS_WEB, "Webhooks", TRIGGERS),
				new ExtTextPropertyDescriptor(Ids.TRIGGERS_IMAGE_CHANGE, "Image Change", TRIGGERS)
		});
	}

	private List<IPropertyDescriptor> getGitBuildSource() {
		return Arrays.asList(new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.SOURCE_TYPE, "Type", SOURCE),
				new ExtTextPropertyDescriptor(Ids.SOURCE_GIT_REF, "Ref", SOURCE),
				new ExtTextPropertyDescriptor(Ids.SOURCE_URI, "URI", SOURCE),
		});
	}

	@Override
	public Object getPropertyValue(Object id) {
		if(id instanceof Ids){
			switch((Ids)id){
			case Type: return getResource().getBuildStrategy().getType();
			case CUSTOM_EXPOSE_DOCKER_SOCKET:
				return getResource().<ICustomBuildStrategy>getBuildStrategy().exposeDockerSocket();
			case CUSTOM_ENV:
				return new KeyValuePropertySource(getResource().<ICustomBuildStrategy>getBuildStrategy().getEnvironmentVariables());
			case CUSTOM_IMAGE:
				return getResource().<ICustomBuildStrategy>getBuildStrategy().getImage();
			case DOCKER_CONTEXT_DIR:
				return getResource().<IDockerBuildStrategy>getBuildStrategy().getContextDir();
			case DOCKER_IMAGE:
				return getResource().<IDockerBuildStrategy>getBuildStrategy().getBaseImage();
			case OUTPUT_REPO_NAME:
				return getResource().getOutputRepositoryName();
			case SOURCE_TYPE:
				return getResource().getBuildSource().getType();
			case SOURCE_URI:
				return getResource().getSourceURI();
			case SOURCE_GIT_REF:
				String ref = getResource().<IGitBuildSource>getBuildSource().getRef();
				return "".equals(ref) ? "master" : ref;
			case STI_SCRIPT_LOCATION: 
				return getResource().<ISTIBuildStrategy>getBuildStrategy().getScriptsLocation();
			case STI_IMAGE:
				return getResource().<ISTIBuildStrategy>getBuildStrategy().getImage();
			case STI_ENV:
				return new KeyValuePropertySource(getResource().<ISTIBuildStrategy>getBuildStrategy().getEnvironmentVariables());
			case TRIGGERS_IMAGE_CHANGE:
				return new ImageChangePropertySource(getResource().getBuildTriggers());
			case TRIGGERS_WEB:
				return new WebHooksPropertySource(getResource().getBuildTriggers());
			default:
			}
		}
		return super.getPropertyValue(id);
	}

	private List<IPropertyDescriptor> getDockerPropertyDescriptors() {
		return Arrays.asList(new  IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.DOCKER_CONTEXT_DIR, "Context Dir", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.DOCKER_IMAGE, IMAGE, STRATEGY),
		});
	}
	
	
	private List<IPropertyDescriptor> getCustomPropertyDescriptors() {
		return Arrays.asList(new  IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_EXPOSE_DOCKER_SOCKET, "Expose Docker Socket", STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_IMAGE, IMAGE, STRATEGY),
				new ExtTextPropertyDescriptor(Ids.CUSTOM_ENV, ENVIRONMENT_VARIABLES, STRATEGY),
		});
	}
	
	private  List<IPropertyDescriptor>  getSTIPropertyDescriptors(){
		return Arrays.asList(new  IPropertyDescriptor[]{
			new ExtTextPropertyDescriptor(Ids.Type, "Type", STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_SCRIPT_LOCATION, "Script Location", STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_IMAGE, IMAGE, STRATEGY),
			new ExtTextPropertyDescriptor(Ids.STI_ENV, ENVIRONMENT_VARIABLES, STRATEGY),
		});
	}
	public static enum Ids{
		Type,
		CUSTOM_EXPOSE_DOCKER_SOCKET, 
		CUSTOM_IMAGE, 
		CUSTOM_ENV,
		DOCKER_CONTEXT_DIR, 
		DOCKER_IMAGE,
		OUTPUT_REPO_NAME,
		STI_SCRIPT_LOCATION,
		STI_IMAGE,
		STI_ENV, 
		SOURCE_TYPE, 
		SOURCE_GIT_REF, 
		SOURCE_URI, 
		TRIGGERS_WEB,
		TRIGGERS_IMAGE_CHANGE
	}
}
