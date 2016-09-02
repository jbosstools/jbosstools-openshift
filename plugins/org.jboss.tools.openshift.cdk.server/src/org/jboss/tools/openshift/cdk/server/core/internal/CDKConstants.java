/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.core.internal;

public interface CDKConstants {
	
	public static final String SPACE = " ";
	public static final String VAGRANT = "vagrant";
	public static final String VAGRANT_CMD_STATUS = "status";
	public static final String VAGRANT_CMD_UP = "up";
	public static final String VAGRANT_CMD_HALT = "halt";
	public static final String VAGRANT_CMD_SERVICE_MANAGER = "service-manager";
	public static final String VAGRANT_CMD_SERVICE_MANAGER_ARG_ENV = "env";
	public static final String VAGRANT_FLAG_MACHINE_READABLE = "--machine-readable";
	public static final String VAGRANT_FLAG_NO_COLOR= "--no-color";
	public static final String VAGRANT_FLAG_PROVIDER_NAME = "provider-name";
	
	public static final String CDK_ENV_SUB_USERNAME = "SUB_USERNAME";
	public static final String CDK_ENV_SUB_PASSWORD = "SUB_PASSWORD";
	public static final String CDK_RESOURCE_VAGRANTFILE = "Vagrantfile";
	public static final String CDK_RESOURCE_DOTCDK= ".cdk";

	
	// Response strings from a status call
	static final String STATE = "state";
	static final String STATE_HUMAN_SHORT = "state-human-short";
	static final String STATE_HUMAN_LONG = "state-human-long";
	static final String STATE_RUNNING = "running";
	static final String STATE_SHUTOFF = "shutoff";
	static final String STATE_POWEROFF = "poweroff";
	static final String STATE_NOT_CREATED = "not_created";
}
