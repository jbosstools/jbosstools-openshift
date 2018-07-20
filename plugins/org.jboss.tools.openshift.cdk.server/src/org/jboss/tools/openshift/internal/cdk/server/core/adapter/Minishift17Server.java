/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter;

import org.eclipse.core.runtime.IProgressMonitor;

public class Minishift17Server extends CDK32Server {
	private static final String MS_17_BASE_NAME = "Minishift 1.7+";
	@Override
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		setAttribute(PROP_PASS_CREDENTIALS, false);
	}

	public static String getServerTypeBaseName() {
		return MS_17_BASE_NAME;
	}
	
	@Override
	protected String getBaseName() {
		return Minishift17Server.getServerTypeBaseName();
	}

	public static boolean matchesMinishift17OrGreater(String version) {
		if (version.contains("+")) {
			String prefix = version.substring(0, version.indexOf("+"));
			String[] segments = prefix.split("\\.");
			if ("1".equals(segments[0]) && Integer.parseInt(segments[1]) >= 7) {
				return true;
			}
		}
		return false;
	}
}
