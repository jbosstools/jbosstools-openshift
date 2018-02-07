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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

public class CDK32Server extends CDK3Server {
	public static final String PROFILE_ID = "minishift.profile";
	public static final String MINISHIFT_DEFAULT_PROFILE = "minishift";

	@Override
	protected String getBaseName() {
		return CDK32Server.getServerTypeBaseName();
	}

	public static String getServerTypeBaseName() {
		return "Container Development Environment 3.2+";
	}

	public static boolean supportsProfiles(IServer server) {
		String[] ok = new String[] { CDK32Server.CDK_V32_SERVER_TYPE, CDK32Server.MINISHIFT_1_7_SERVER_TYPE };
		List<String> valid = Arrays.asList(ok);
		return valid.contains(server.getServerType().getId());
	}

	public static String[] getArgsWithProfile(IServer server, String[] args) {
		if (supportsProfiles(server)) {
			String profileName = server.getAttribute(CDK32Server.PROFILE_ID, (String) null);
			if (StringUtils.isEmpty(profileName)) {
				profileName = CDK32Server.MINISHIFT_DEFAULT_PROFILE;
			}
			if (profileName != null) {
				List<String> al = new ArrayList<String>();
				al.add("--profile");
				al.add(profileName);
				al.addAll(Arrays.asList(args));
				return (String[]) al.toArray(new String[al.size()]);
			}
		}
		return args;
	}

	public static boolean matchesCDK32(String version) {
		return version.startsWith("3.") && !(CDK3Server.matchesCDK3(version));
	}

}
