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
import org.jboss.tools.cdk.reddeer.core.enums.CDKServerAdapterType;

/**
 * Enum holding information for CDK/Minshift container runtimes
 * @author odockal
 *
 */
public enum CDKVersion {
	
	CDK2X 	(CDKServerAdapterType.CDK2, "2.x", CDKLabel.Server.CDK_SERVER_NAME, ""),
	CDK300 	(CDKServerAdapterType.CDK3, "3.0.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.0-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK310 	(CDKServerAdapterType.CDK3, "3.1.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK311 	(CDKServerAdapterType.CDK3, "3.1.1", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.1-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK320 	(CDKServerAdapterType.CDK32, "3.2.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.2.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK330 	(CDKServerAdapterType.CDK32, "3.3.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.3.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK340 	(CDKServerAdapterType.CDK32, "3.4.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.4.0-2-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK350 	(CDKServerAdapterType.CDK32, "3.5.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.5.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	MINISHIFT1140 (CDKServerAdapterType.MINISHIFT17, "1.14.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.14.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1151 (CDKServerAdapterType.MINISHIFT17, "1.15.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.15.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1161 (CDKServerAdapterType.MINISHIFT17, "1.16.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.16.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1170 (CDKServerAdapterType.MINISHIFT17, "1.17.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.17.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1180 (CDKServerAdapterType.MINISHIFT17, "1.18.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.18.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1190 (CDKServerAdapterType.MINISHIFT17, "1.19.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.19.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1200 (CDKServerAdapterType.MINISHIFT17, "1.20.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.20.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1210 (CDKServerAdapterType.MINISHIFT17, "1.21.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.21.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch());
	
	private final CDKServerAdapterType type;
	private final String version;
	private final String serverName;
	private final String downloadName;
	
	CDKVersion(CDKServerAdapterType type, String version, String serverName, String downloadName) {
		this.type = type;
		this.version = version;
		this.serverName = serverName;
		this.downloadName = downloadName;
	}
	
	public CDKServerAdapterType type() { 
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
		return this.type.serverTypeName() + " v" + this.version;
	}
	
	private static String getArch() {
		return CDKLabel.Others.FILE_ARCH;
	}
	
}
