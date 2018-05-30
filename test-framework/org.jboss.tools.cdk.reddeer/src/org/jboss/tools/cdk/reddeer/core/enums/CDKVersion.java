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
package org.jboss.tools.cdk.reddeer.core.enums;

import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Enum holding information for CDK/Minshift container runtimes
 * @author odockal
 *
 */
public enum CDKVersion {
	
	CDK2X 	("CDK", "2.x", CDKLabel.Server.CDK_SERVER_NAME, ""),
	CDK300 	("CDK", "3.0.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.0-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	CDK310 	("CDK", "3.1.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	CDK311 	("CDK", "3.1.1", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.1-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	CDK320 	("CDK", "3.2.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.2.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	CDK330 	("CDK", "3.3.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.3.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	CDK340 	("CDK", "3.4.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.4.0-2-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64" + CDKRuntimeOS.get().getSuffix()),
	MINISHIFT1140 ("Minishift", "1.14.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.14.0-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64"),
	MINISHIFT1151 ("Minishift", "1.15.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.15.1-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64"),
	MINISHIFT1161 ("Minishift", "1.16.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.16.1-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64"),
	MINISHIFT1170 ("Minishift", "1.17.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.17.0-" + CDKRuntimeOS.get().getRuntimeFullName() + "-amd64");
	
	private final String type;
	private final String version;
	private final String serverName;
	private final String downloadName;
	
	CDKVersion(String type, String version, String serverName, String downloadName) {
		this.type = type;
		this.version = version;
		this.serverName = serverName;
		this.downloadName = downloadName;
	}
	
	public String type() { 
		return this.type; 
	}
	
	public String version() { 
		return this.version; 
	}
	
	public String serverName() {
		return serverName;
	}
	
	public String downloadName() {
		return downloadName;
	}
	
	public String getTypeAndVersion() {
		return this.type + " v" + this.version;
	}
	
}
