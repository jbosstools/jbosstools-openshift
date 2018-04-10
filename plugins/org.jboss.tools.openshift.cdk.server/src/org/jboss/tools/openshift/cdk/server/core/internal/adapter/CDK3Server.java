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
package org.jboss.tools.openshift.cdk.server.core.internal.adapter;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

public class CDK3Server extends CDKServer {

	// virtualbox vmwarefusion kvm xhyve hyperv
	public static final String VIRTUALBOX = "virtualbox";
	public static final String VMWAREFUSION = "vmwarefusion";
	public static final String KVM = "kvm";
	public static final String XHYVE = "xhyve";
	public static final String HYPERV = "hyperv";

	public static final String PROP_HYPERVISOR = "org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer.hypervisor";
	public static final String MINISHIFT_FILE = "minishift.file.location";
	public static final String MINISHIFT_HOME = "minishift.home.location";
	public static final String ENV_MINISHIFT_HOME = "MINISHIFT_HOME";

	public static final String[] getHypervisors() {
		return getHypervisors(Platform.getOS());
	}

	public static final String[] getHypervisors(String platform) {
		if (Platform.OS_WIN32.equals(platform)) {
			return new String[] { HYPERV, VIRTUALBOX };
		}
		if (Platform.OS_LINUX.equals(platform)) {
			return new String[] { KVM, VIRTUALBOX };
		}
		if (Platform.OS_MACOSX.equals(platform)) {
			return new String[] { XHYVE, VIRTUALBOX, VMWAREFUSION };
		}
		return new String[] { VIRTUALBOX, VMWAREFUSION, KVM, XHYVE, HYPERV };
	}

	protected String getBaseName() {
		return CDK3Server.getServerTypeBaseName();
	}

	public static String getServerTypeBaseName() {
		return "Container Development Environment 3";
	}

	@Override
	public String getUserEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK3_ENV_SUB_USER_KEY);
	}

	@Override
	public String getPasswordEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK3_ENV_SUB_PASS_KEY);
	}

	public String getMinishiftHome() {
		String home = System.getProperty("user.home");
		String defaultMinishiftHome = new File(home, CDKConstants.CDK_RESOURCE_DOTMINISHIFT).getAbsolutePath();
		String msHome = getServer().getAttribute(CDK3Server.MINISHIFT_HOME, defaultMinishiftHome);
		if (StringUtils.isEmpty(msHome))
			msHome = defaultMinishiftHome;
		return msHome;
	}

	public static boolean matchesCDK3(String version) {
		return (version.startsWith("3.0.") || version.startsWith("3.1."));
	}

	/**
	 * Subclasses may override if the structure of the minishift home folder changes in future versions
	 * @return
	 */
	public boolean isCDKInitialized() {
		String home = getMinishiftHome();
		File homeF = new File(home);
		if (homeF.exists() && homeF.isDirectory()) {
			File cdk = new File(homeF, "cdk");
			File config = new File(homeF, "config");
			File cache = new File(homeF, "cache");
			File configJSON = new File(config, "config.json");
			if (cdk.exists() && config.exists() && cache.exists() && configJSON.exists()) {
				return true;
			}
		}
		return false;
	}
	
}
