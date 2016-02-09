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
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;

import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.build.IBuildSource;
import com.openshift.restclient.model.build.IBuildStatus;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.IGitBuildSource;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

public class BuildPropertySource extends ResourcePropertySource<IBuild> {

	public BuildPropertySource(IBuild resource) {
		super(resource);
	}

	
	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
			new UneditablePropertyDescriptor("status", "Status"),
			new UneditablePropertyDescriptor("started", "Started"),
			new UneditablePropertyDescriptor("duration", "Duration"),
			new UneditablePropertyDescriptor("build.config", "Build Configuration"),
			new UneditablePropertyDescriptor("build.strategy", "Build Strategy"),
			new UneditablePropertyDescriptor("builder.image", "Builder Image"),
			new UneditablePropertyDescriptor("source.type", "Source Type"),
			new UneditablePropertyDescriptor("source.repo", "Source Repo"),
			new UneditablePropertyDescriptor("source.ref", "Source Ref."),
			new UneditablePropertyDescriptor("source.contextDir", "Source Context Dir."),
			new UneditablePropertyDescriptor("output.image", "Output Image"),
			new UneditablePropertyDescriptor("push.secret", "Push Secret")
		};
	}


	@Override
	public Object getPropertyValue(Object id) {
		IBuild build = getResource();
		switch((String)id) {
		case "status": return build.getStatus();
		case "started": 
			return DateTimeUtils.formatSince(build .getCreationTimeStamp());
		case "build.config": 
			return build.getLabels().get(OpenShiftAPIAnnotations.BUILD_CONFIG_NAME);
		case "build.strategy" :
		case "builder.image" :
			return handleBuildStrategy((String)id, build.getBuildStrategy());
		case "source.type" : 
		case "source.repo" : 
		case "source.ref" : 
		case "source.contextDir" :
			return handleBuildSource((String) id, build.getBuildSource());
		case "duration": 
		case "output.image": 
			return handleBuildStatus((String)id, build.getBuildStatus());
		case "push.secret" :
			return build.getPushSecret();
		}
		return super.getPropertyValue(id);
	}
	
	private Object handleBuildStatus(String id, IBuildStatus status) {
		if(status != null) {
			switch(id){
			case "output.image":
				return status.getOutputDockerImage() != null ? status.getOutputDockerImage().getUriWithoutHost() : "";
			case "duration":
				return DateTimeUtils.formatDuration(status.getDuration());
			}
		}
		return "";
	}
	
	private Object handleBuildStrategy(String id, IBuildStrategy strategy) {
		if(strategy == null) return "";
		switch(id) {
		case "build.strategy" : return strategy.getType();
		case "builder.image" : 
			if(strategy instanceof IDockerBuildStrategy) {
				return ((IDockerBuildStrategy) strategy).getBaseImage();
			}
			if(strategy instanceof ISourceBuildStrategy) {
				return ((ISourceBuildStrategy) strategy).getImage();
			}
		}
		return "";
	}
	
	private Object handleBuildSource(String id, IBuildSource buildSource) {
		if(buildSource == null) return "";
		if("source.type".equals(id)) return buildSource.getType();
		if(buildSource instanceof IGitBuildSource) {
			IGitBuildSource source = (IGitBuildSource) buildSource;
			switch(id) {
			case "source.repo":  return source.getURI();
			case "source.ref":   return source.getRef();
			case "source.contextDir": return source.getContextDir();
			}
		}
		return "";
		
	}
}
