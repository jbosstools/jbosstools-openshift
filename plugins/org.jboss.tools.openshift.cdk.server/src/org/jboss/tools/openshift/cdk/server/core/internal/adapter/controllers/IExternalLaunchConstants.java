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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers;

public interface IExternalLaunchConstants {

	public static final String EXTERNAL_TOOLS = "org.eclipse.ui.externaltools.ProgramLaunchConfigurationType";
	public static final String ATTR_LOCATION = "org.eclipse.ui.externaltools.ATTR_LOCATION";
	public static final String ATTR_ARGS = "org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS";
	public static final String ATTR_WORKING_DIR = "org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY";
	public static final String ENVIRONMENT_VARS_KEY = "org.eclipse.debug.core.environmentVariables";


}
