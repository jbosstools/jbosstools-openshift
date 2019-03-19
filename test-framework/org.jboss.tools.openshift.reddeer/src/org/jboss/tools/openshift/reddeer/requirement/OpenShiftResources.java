/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

/**
 * @author adietish@redhat.com
 */
public class OpenShiftResources {

	/**
	 * the openshift project that holds templates
	 */
	public static final String OPENSHIFT_PROJECT = "openshift";

	/**
	 * the template that creates a basic eap service
	 */
	public static final String EAP_TEMPLATE = "eap72-basic-s2i";
	public static final String EAP_SERVICE = "eap-app";
	public static final String EAP_SERVICE_PING = "eap-app-ping";
	public static final String EAP_APP_REPLICATION_CONTROLLER = "eap-app-1";
	public static final String EAP_APP_GIT_NAME = "jboss-eap-quickstarts";
	public static final String EAP_TEMPLATE_RESOURCES_PATH = "resources/eap72-basic-s2i-helloworld.json";
	public static final String EAP_TEMPLATE_RESOURCES_FILENAME = "eap72-basic-s2i-helloworld.json";
	
	public static final String EAP_CD_TEMPLATE = "resources/eap-cd-basic-s2i-helloworld.json";

	/**	
	 * the template that creates a basic nodejs service
	 */
	public static final String NODEJS_TEMPLATE = "https://raw.githubusercontent.com/sclorg/nodejs-ex/master/openshift/templates/nodejs.json";
	public static final String NODEJS_SERVICE = "nodejs-example";
	public static final String NODEJS_APP_REPLICATION_CONTROLLER = "nodejs-example-1";
	public static final String NODEJS_APP_DEPLOYMENT_CONFIG = "nodejs-example";
	public static final String NODEJS_GIT_NAME = "nodejs-ex";
}
