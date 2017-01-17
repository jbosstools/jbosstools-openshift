/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.enums;

public enum Resource {

	BUILD_CONFIG("Build Configs"),
	BUILD("Builds"),
	DEPLOYMENT_CONFIG("Deployment Configs"),
	IMAGE_STREAM("Image Streams"),
	POD("Pods"),
	ROUTE("Routes"),
	SERVICE("Services"),
	TEMPLATE("Templates"),
	DEPLOYMENT("Deployments");
	
	private final String text;
	
	private Resource(String text) {
		this.text = text;
	}
	
	@Override
	public String toString() {
		return text;
	}
	
}
