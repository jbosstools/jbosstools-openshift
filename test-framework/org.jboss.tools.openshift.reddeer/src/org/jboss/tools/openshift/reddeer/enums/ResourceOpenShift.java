/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.enums;

/**
 * Enum represents OpenShift resources
 * 
 * @author jkopriva@redhat.com
 *
 */
public enum ResourceOpenShift {

	BUILD_CONFIG("BuildConfig"),
	BUILD("Build"),
	DEPLOYMENT_CONFIG("DeploymentConfig"),
	IMAGE_STREAM("ImageStream"),
	POD("Pod"),
	ROUTE("Route"),
	SERVICE("Service"),
	TEMPLATE("Template"),
	DEPLOYMENT("Deployment"),
	REPLICATION_CONTROLLER("ReplicationController");
	
	private final String text;
	
	private ResourceOpenShift(String text) {
		this.text = text;
	}
	
	@Override
	public String toString() {
		return text;
	}
	
}
