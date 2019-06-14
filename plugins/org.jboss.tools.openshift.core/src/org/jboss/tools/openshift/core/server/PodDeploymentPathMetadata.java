/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class PodDeploymentPathMetadata {

	// default fallback
	private static final String DEFAULT_DEPLOYMENT_DIR = "/opt/app-root/src";
	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_REDHAT_DEPLOYMENTS_DIR = Pattern
			.compile("\"com\\.redhat\\.deployments-dir\"[^\"]*\"([^\"]*)\",");
	// "image->"dockerImageMetadata"->"Config"->"Labels"->"com.redhat.deployments-dir"
	private static final Pattern PATTERN_JBOSS_DEPLOYMENTS_DIR = Pattern
			.compile("\"org\\.jboss\\.deployments-dir\"[^\"]*\"([^\"]*)\",");
	// "image->"dockerImageMetadata"->"Config"->"WorkginDir"
	private static final Pattern PATTERN_WOKRING_DIR = Pattern.compile("\"WorkingDir\"[^\"]*\"([^\"]*)\",");

	private String metadata;

	public PodDeploymentPathMetadata(String metadata) {
		this.metadata = metadata;
	}

	public String get() {
		if (StringUtils.isEmpty(metadata)) {
			return null;
		}
		return getPodPath(metadata);
	}

	protected String useDefaultPathIfEmpty(String podPath) {
		if (StringUtils.isEmpty(podPath)) {
			return DEFAULT_DEPLOYMENT_DIR;
		}
		return podPath;
	}

	private String getPodPath(String imageMetaData) {
		String podPath = null;
		if ((podPath = matchFirstGroup(imageMetaData, PATTERN_REDHAT_DEPLOYMENTS_DIR)) == null
				&& (podPath = matchFirstGroup(imageMetaData, PATTERN_JBOSS_DEPLOYMENTS_DIR)) == null) {
			podPath = matchFirstGroup(imageMetaData, PATTERN_WOKRING_DIR);
		}

		return podPath;
	}

	private String matchFirstGroup(String imageStreamTag, Pattern pattern) {
		Matcher matcher = pattern.matcher(imageStreamTag);
		if (matcher.find() && matcher.groupCount() == 1) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

}
