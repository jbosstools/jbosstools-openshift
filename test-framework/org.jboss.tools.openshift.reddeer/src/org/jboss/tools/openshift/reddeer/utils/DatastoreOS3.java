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
package org.jboss.tools.openshift.reddeer.utils;

import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.KEY_SERVER;
import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.KEY_USERNAME;
import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.getRequiredProperty;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView.AuthenticationMethod;

/**
 * Storage for settings that are used in the OpenShift 3 integration tests.
 * 
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 *
 */
public class DatastoreOS3 {
	
	private static final String KEY_AUTHMETHOD = "openshift.authmethod";
	private static final String KEY_TOKEN = "openshift.token";
	public static final String KEY_PASSWORD = "openshift.password";
	public static final String KEY_NEXUS_MIRROR = "openshift.nexus.mirror";
	
	static {
		AuthenticationMethod authMethod = AuthenticationMethod.safeValueOf(
				getRequiredProperty(KEY_AUTHMETHOD, 
						new String[] { "basic", "oauth" }, 
						"Please add '-D" + KEY_AUTHMETHOD + "=[basic|oauth]' to your launch arguments"));
		if (AuthenticationMethod.BASIC.equals(authMethod)) { 
			assertTrue("Please add '-D" + KEY_PASSWORD + "=[password]'", 
					StringUtils.isNotBlank(System.getProperty(KEY_PASSWORD)));
		} else if (AuthenticationMethod.OAUTH.equals(authMethod)) {
			assertTrue("Please add '-D" + KEY_TOKEN + "=[token]' to your launch arguments", 
					StringUtils.isNotBlank(System.getProperty(KEY_TOKEN)));
		}
	}

	// used for basic authentication
	public static final String SERVER = getRequiredProperty(KEY_SERVER,
			"Please add '-D" + KEY_SERVER + "=[host]' to your launch arguments");
	public static String USERNAME = System.getProperty(KEY_USERNAME);
	public static String PASSWORD = System.getProperty(KEY_PASSWORD);
	public static String TOKEN = System.getProperty(KEY_TOKEN);
	public static String PUBLIC_OS3_SERVER = "https://console.preview.openshift.com";
	public static String NEXUS_MIRROR_URL = System.getProperty(KEY_NEXUS_MIRROR);
	public static AuthenticationMethod AUTH_METHOD = AuthenticationMethod.valueOfIgnoreCase(System.getProperty(KEY_AUTHMETHOD));
	
	// github credentials
	public static final String GIT_USERNAME = System.getProperty("github.username", "openshift-tools-testing-account");
	public static final String GIT_PASSWORD = System.getProperty("github.password");
	
	public static String PROJECT1 = "project-name01-" + System.currentTimeMillis();
	public static String PROJECT1_DISPLAYED_NAME = "displayedName-" + System.currentTimeMillis();
	public static String PROJECT2 = "project-name02-" + System.currentTimeMillis();
	public static final String TEST_PROJECT = "test-project";

	
	public static String TEMPLATE_PATH = new File("").getAbsolutePath() 
			+ File.separator + "resources" 
			+ File.separator + "eap64-basic-s2i.json";
	
	/**
	 * Generates a new project name for DatastoreOS3.PROJECT1 variable.
	 */
	public static void generateProjectName() {
		long seed = System.currentTimeMillis();
		PROJECT1 = "project-name01-" + seed;
		PROJECT1_DISPLAYED_NAME = "displayedName-" + seed;
	}

	public static void generateProject2Name() {
		long seed = System.currentTimeMillis();
		PROJECT2 = "project-name02-" + seed;
	}

}
