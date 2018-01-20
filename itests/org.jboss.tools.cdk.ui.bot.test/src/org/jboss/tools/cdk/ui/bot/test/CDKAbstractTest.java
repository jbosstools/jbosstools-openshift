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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.hamcrest.Matcher;
import org.jboss.tools.cdk.reddeer.core.enums.CDKHypervisor;
import org.jboss.tools.cdk.reddeer.core.matcher.JobMatcher;
import org.jboss.tools.cdk.ui.bot.test.utils.CDKTestUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

public abstract class CDKAbstractTest {

	// Server Adapter Wizard strings - Server artifacts
	public static final String SERVER_HOST = "localhost"; //$NON-NLS-1$
	public static final String SERVER_TYPE_GROUP = "Red Hat JBoss Middleware"; //$NON-NLS-1$
	public static final String CREDENTIALS_DOMAIN = "access.redhat.com"; //$NON-NLS-1$
	public static final String CDK_SERVER_NAME = "Red Hat Container Development Kit 2.x"; //$NON-NLS-1$
	public static final String CDK3_SERVER_NAME = "Red Hat Container Development Kit 3"; //$NON-NLS-1$
	public static final String CDK32_SERVER_NAME = "Red Hat Container Development Kit 3.2+"; //$NON-NLS-1$
	public static final String MINISHIFT_SERVER_NAME = "Minishift 1.7+";
	
	// Server adapter artifacts
	public static final String SERVER_ADAPTER = "Container Development Environment"; //$NON-NLS-1$
	public static final String SERVER_ADAPTER_3 = "Container Development Environment 3"; //$NON-NLS-1$
	public static final String SERVER_ADAPTER_32 = "Container Development Environment 3.2+"; //$NON-NLS-1$
	public static final String SERVER_ADAPTER_MINISHIFT = "Minishift 1.7+";
	
	// General parameters
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
	public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
	public static final String FOLDER = IS_LINUX ? "linux" : (IS_WINDOWS ? "win" : "mac");
	
	public static final String MINISHIFT_VALIDATION_JOB = "Validate minishift location";
	
	public static final String separator = System.getProperty("file.separator");
	
	// CDK Test suite parameters
	public static final String USERNAME;
	public static final String PASSWORD;
	public static final String MINISHIFT_HYPERVISOR;
	public static final String MINISHIFT;
	public static final String CDK_MINISHIFT;
	public static final String CDK32_MINISHIFT;
	public static final String DEFAULT_MINISHIFT_HOME;
	
	static {
		USERNAME = CDKTestUtils.getSystemProperty("developers.username"); //$NON-NLS-1$
		PASSWORD = CDKTestUtils.getSystemProperty("developers.password"); //$NON-NLS-1$
		MINISHIFT = CDKTestUtils.getSystemProperty("minishift"); //$NON-NLS-1$
		CDK_MINISHIFT = CDKTestUtils.getSystemProperty("cdk.minishift"); //$NON-NLS-1$
		CDK32_MINISHIFT = CDKTestUtils.getSystemProperty("cdk32.minishift"); //$NON-NLS-1$
		MINISHIFT_HYPERVISOR = assignMinishiftHypervisor();
		DEFAULT_MINISHIFT_HOME = System.getProperty("user.home") + separator + ".minishift";
	}
	
	public static Matcher<?> getJobMatcher(String title) {
		return new JobMatcher(new Job(title) {

			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		});
	}
	
	public static String assignMinishiftHypervisor() {
		String prop = CDKTestUtils.getSystemProperty("hypervisor");
		return StringUtils.isEmptyOrNull(prop) ? CDKHypervisor.getDefaultHypervisor().toString() : prop;
	}
	
	public static void checkCDKParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("CDK 3.X path", CDK_MINISHIFT);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkMinishiftParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Minishift path", MINISHIFT);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkCDK32Parameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("CDK 3.2+ path", CDK32_MINISHIFT);
		CDKTestUtils.checkParameterNotNull(dict);
	}
	
	public static void checkDevelopersParameters() {
		Map<String, String> dict = new HashMap<>();
		dict.put("Username", USERNAME);
		dict.put("Password", PASSWORD);
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
