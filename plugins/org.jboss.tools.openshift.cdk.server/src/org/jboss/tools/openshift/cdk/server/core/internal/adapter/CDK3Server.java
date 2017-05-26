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

import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;

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
	
	
	public static final String[] getHypervisors() {
		return getHypervisors(Platform.getOS());
	}

	public static final String[] getHypervisors(String platform) {
		if( Platform.OS_WIN32.equals(platform)) {
			return new String[]{HYPERV, VIRTUALBOX};
		}
		if( Platform.OS_LINUX.equals(platform)) {
			return new String[]{KVM, VIRTUALBOX};
		}
		if( Platform.OS_MACOSX.equals(platform)) {
			return new String[]{XHYVE, VIRTUALBOX, VMWAREFUSION};
		}
		return new String[]{
				VIRTUALBOX, VMWAREFUSION, KVM, XHYVE, HYPERV
		};
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
	
}
