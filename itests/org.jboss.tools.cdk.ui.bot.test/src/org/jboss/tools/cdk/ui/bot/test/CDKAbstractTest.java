/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;

public abstract class CDKAbstractTest {

	public static final String SERVER_HOST = "localhost"; //$NON-NLS-1$
	
	public static final String SERVER_TYPE_GROUP = "Red Hat JBoss Middleware"; //$NON-NLS-1$
	
	public static final String CREDENTIALS_DOMAIN = "access.redhat.com"; //$NON-NLS-1$
	
	public static final String CDK_SERVER_NAME = "Red Hat Container Development Kit 2.x"; //$NON-NLS-1$

	public static final String CDK3_SERVER_NAME = "Red Hat Container Development Kit 3"; //$NON-NLS-1$
	
	public static final String CDK32_SERVER_NAME = "Red Hat Container Development Kit 3.2+"; //$NON-NLS-1$
	
	public static final String SERVER_ADAPTER = "Container Development Environment"; //$NON-NLS-1$
	
	public static final String SERVER_ADAPTER_3 = "Container Development Environment 3"; //$NON-NLS-1$
	
	public static final String SERVER_ADAPTER_32 = "Container Development Environment 3.2+"; //$NON-NLS-1$
	
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
	
	public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
	
	public static final String FOLDER = IS_LINUX ? "linux" : (IS_WINDOWS ? "win" : "mac");
	
	public static final String separator = System.getProperty("file.separator");
	
	public static final String USERNAME;
	
	public static final String PASSWORD;
	
	public static final String VAGRANTFILE;
	
	public static final String MINISHIFT_HYPERVISOR;
	
	public static final String MINISHIFT;
	
	public static final String MINISHIFT_PROFILE;
	
	public static final String DEFAULT_MINISHIFT_HOME;
	
	static {
		USERNAME = CDKTestUtils.getSystemProperty("developers.username"); //$NON-NLS-1$
		PASSWORD = CDKTestUtils.getSystemProperty("developers.password"); //$NON-NLS-1$
		VAGRANTFILE = CDKTestUtils.getSystemProperty("vagrantfile"); //$NON-NLS-1$
		MINISHIFT = CDKTestUtils.getSystemProperty("minishift"); //$NON-NLS-1$
		MINISHIFT_PROFILE = CDKTestUtils.getSystemProperty("minishift.profile"); //$NON-NLS-1$
		MINISHIFT_HYPERVISOR = CDKTestUtils.getSystemProperty("minishift.hypervisor"); //$NON-NLS-1$
		DEFAULT_MINISHIFT_HOME = System.getProperty("user.home") + separator + ".minishift";
	}
	
	public static void checkMinishiftParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Minishift path", MINISHIFT);
		dict.put("Minishift hypervisor", MINISHIFT_HYPERVISOR == null ? "" : MINISHIFT_HYPERVISOR);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkMinishiftProfileParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Minishift profile path", MINISHIFT_PROFILE);
		dict.put("Minishift hypervisor", MINISHIFT_HYPERVISOR == null ? "" : MINISHIFT_HYPERVISOR);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkDevelopersParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Username", USERNAME);
		dict.put("Password", PASSWORD);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkVagrantfileParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Vagrantfile path", VAGRANTFILE);
		CDKTestUtils.checkParameterNotNull(dict);		
	}
	
	/**
	 * Provide resource absolute path in project directory
	 * @param path - resource relative path
	 * @return resource absolute path
	 */
	public static String getProjectAbsolutePath(String... path) {

		// Construct path
		StringBuilder builder = new StringBuilder();
		for (String fragment : path) {
			builder.append("/" + fragment);
		}

		String filePath = "";
		filePath = System.getProperty("user.dir");
		File file = new File(filePath + builder.toString());
		if (!file.exists()) {
			throw new RedDeerException("Resource file does not exists within project path "
					+ filePath + builder.toString());
		}

		return file.getAbsolutePath();
	}
}
