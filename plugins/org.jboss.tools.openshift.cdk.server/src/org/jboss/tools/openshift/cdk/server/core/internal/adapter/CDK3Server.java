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

import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;

public class CDK3Server extends CDKServer {

	protected String getServerTypeBaseName() {
		return "Container Development Environment 3";
	}
	
	public String getUserEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKConstants.CDK3_ENV_SUB_USERNAME);
	}
	
	public String getPasswordEnvironmentKey() {
		return getServer().getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKConstants.CDK3_ENV_SUB_PASSWORD);
	}
	
}
