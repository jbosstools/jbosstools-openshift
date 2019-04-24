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

	private static final String PUSH_SECRET = "push.secret";
	private static final String STARTED = "started";
	private static final String DURATION = "duration";
	private static final String BUILD_CONFIG = "build.config";
	private static final String BUILD_STRATEGY = "build.strategy";
	private static final String BUILDER_IMAGE = "builder.image";
	private static final String SOURCE_TYPE = "source.type";
	private static final String SOURCE_REPO = "source.repo";
	private static final String OUTPUT_IMAGE = "output.image";
	private static final String SOURCE_CONTEXT_DIR = "source.contextDir";
	private static final String SOURCE_REF = "source.ref";

	public BuildPropertySource(IBuild resource) {
		super(resource);
	}

	@Override
	public IPropertyDescriptor[] getResourcePropertyDescriptors() {
		return new IPropertyDescriptor[] { new UneditablePropertyDescriptor("status", "Status"),
				new UneditablePropertyDescriptor(STARTED, "Started"),
				new UneditablePropertyDescriptor(DURATION, "Duration"),
				new UneditablePropertyDescriptor(BUILD_CONFIG, "Build Configuration"),
				new UneditablePropertyDescriptor(BUILD_STRATEGY, "Build Strategy"),
				new UneditablePropertyDescriptor(BUILDER_IMAGE, "Builder Image"),
				new UneditablePropertyDescriptor(SOURCE_TYPE, "Source Type"),
				new UneditablePropertyDescriptor(SOURCE_REPO, "Source Repo"),
				new UneditablePropertyDescriptor(SOURCE_REF, "Source Ref."),
				new UneditablePropertyDescriptor(SOURCE_CONTEXT_DIR, "Source Context Dir."),
				new UneditablePropertyDescriptor(OUTPUT_IMAGE, "Output Image"),
				new UneditablePropertyDescriptor(PUSH_SECRET, "Push Secret") };
	}

	@Override
	public Object getPropertyValue(Object id) {
		IBuild build = getResource();
		if (id instanceof String) {
			switch ((String) id) {
			case "status":
				return build.getStatus();
			case STARTED:
				return DateTimeUtils.formatSince(build.getCreationTimeStamp());
			case BUILD_CONFIG:
				return build.getLabels().get(OpenShiftAPIAnnotations.BUILD_CONFIG_NAME);
			case BUILD_STRATEGY:
			case BUILDER_IMAGE:
				return handleBuildStrategy((String) id, build.getBuildStrategy());
			case SOURCE_TYPE:
			case SOURCE_REPO:
			case SOURCE_REF:
			case SOURCE_CONTEXT_DIR:
				return handleBuildSource((String) id, build.getBuildSource());
			case DURATION:
			case OUTPUT_IMAGE:
				return handleBuildStatus((String) id, build.getBuildStatus());
			case PUSH_SECRET:
				return build.getPushSecret();
			}
		}
		return super.getPropertyValue(id);
	}

	private Object handleBuildStatus(String id, IBuildStatus status) {
		if (status != null) {
			switch (id) {
			case OUTPUT_IMAGE:
				return status.getOutputDockerImage() != null ? status.getOutputDockerImage().getUriWithoutHost() : "";
			case DURATION:
				return DateTimeUtils.formatDuration(status.getDuration());
			}
		}
		return "";
	}

	private Object handleBuildStrategy(String id, IBuildStrategy strategy) {
		if (strategy == null)
			return "";
		switch (id) {
		case BUILD_STRATEGY:
			return strategy.getType();
		case BUILDER_IMAGE:
			if (strategy instanceof IDockerBuildStrategy) {
				return ((IDockerBuildStrategy) strategy).getBaseImage();
			}
			if (strategy instanceof ISourceBuildStrategy) {
				return ((ISourceBuildStrategy) strategy).getImage();
			}
			return "";
		default:
			return "";
		}
	}

	private Object handleBuildSource(String id, IBuildSource buildSource) {
		if (buildSource == null)
			return "";
		if (SOURCE_TYPE.equals(id))
			return buildSource.getType();
		if (buildSource instanceof IGitBuildSource) {
			IGitBuildSource source = (IGitBuildSource) buildSource;
			switch (id) {
			case SOURCE_REPO:
				return source.getURI();
			case SOURCE_REF:
				return source.getRef();
			case SOURCE_CONTEXT_DIR:
				return source.getContextDir();
			}
		}
		return "";

	}
}
