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
package org.jboss.tools.openshift.cdk.server.core.internal.detection;

import java.io.File;

import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDK32RuntimeDetector extends CDK3RuntimeDetector {
	protected boolean matchesExpectedVersion(String version) {
		boolean v3 = version.startsWith("3.");
		boolean not30or31 = !(version.startsWith("3.0.") || version.startsWith("3.1."));
		if( v3 && not30or31) 
			return true;
		return false;
	}
	
	@Override
	protected String getServerType() {
		return CDKServer.CDK_V32_SERVER_TYPE;
	}
	

	@Override
	protected String getDefinitionName(File root) {
		return CDK32Server.getServerTypeBaseName();
	}
}
