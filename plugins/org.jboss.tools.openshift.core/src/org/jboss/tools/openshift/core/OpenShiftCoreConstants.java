/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

/**
 * @author Jeff MAURY
 *
 */
public class OpenShiftCoreConstants {
	public static final String ODO_CONFIG_YAML = ".odo/config.yaml";
	public static final String HOME_FOLDER = System.getProperty("user.home");

	public static final String OCP4_CONFIG_NAMESPACE = "openshift-config-managed";
	public static final String OCP4_CONSOLE_PUBLIC_CONFIG_MAP_NAME = "console-public";
	public static final String OCP4_CONSOLE_URL_KEY_NAME = "consoleURL";

	public static final String OCP3_CONFIG_NAMESPACE = "openshift-web-console";
	public static final String OCP3_WEBCONSOLE_CONFIG_MAP_NAME = "webconsole-config";
	public static final String OCP3_WEBCONSOLE_YAML_FILE_NAME = "webconsole-config.yaml";
}
