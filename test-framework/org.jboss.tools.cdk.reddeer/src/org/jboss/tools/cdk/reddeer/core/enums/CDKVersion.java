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
	
	CDK2X 	(CDKServerAdapterType.CDK2, "2.x", CDKLabel.Server.CDK_SERVER_NAME, ""),
	CDK300 	(CDKServerAdapterType.CDK3, "3.0.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.0-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK310 	(CDKServerAdapterType.CDK3, "3.1.0", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK311 	(CDKServerAdapterType.CDK3, "3.1.1", CDKLabel.Server.CDK3_SERVER_NAME, "cdk-3.1.1-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK320 	(CDKServerAdapterType.CDK32, "3.2.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.2.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK330 	(CDKServerAdapterType.CDK32, "3.3.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.3.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK340 	(CDKServerAdapterType.CDK32, "3.4.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.4.0-2-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK350 	(CDKServerAdapterType.CDK32, "3.5.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.5.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK360 	(CDKServerAdapterType.CDK32, "3.6.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.6.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK370 	(CDKServerAdapterType.CDK32, "3.7.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.7.0-2-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK380 	(CDKServerAdapterType.CDK32, "3.8.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.8.0-2-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK390 	(CDKServerAdapterType.CDK32, "3.9.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.9.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK3100 (CDKServerAdapterType.CDK32, "3.10.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.10.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK3110 (CDKServerAdapterType.CDK32, "3.11.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.11.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),	
	CDK3120 (CDKServerAdapterType.CDK32, "3.12.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.12.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK3130 (CDKServerAdapterType.CDK32, "3.13.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.13.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK3140 (CDKServerAdapterType.CDK32, "3.14.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.14.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	CDK3150 (CDKServerAdapterType.CDK32, "3.15.0", CDKLabel.Server.CDK32_SERVER_NAME, "cdk-3.15.0-1-minishift-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch() + CDKRuntimeOS.get().getSuffix()),
	MINISHIFT1140 (CDKServerAdapterType.MINISHIFT17, "1.14.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.14.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1151 (CDKServerAdapterType.MINISHIFT17, "1.15.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.15.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1161 (CDKServerAdapterType.MINISHIFT17, "1.16.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.16.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1170 (CDKServerAdapterType.MINISHIFT17, "1.17.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.17.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1180 (CDKServerAdapterType.MINISHIFT17, "1.18.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.18.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1190 (CDKServerAdapterType.MINISHIFT17, "1.19.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.19.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1200 (CDKServerAdapterType.MINISHIFT17, "1.20.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.20.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1210 (CDKServerAdapterType.MINISHIFT17, "1.21.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.21.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1220 (CDKServerAdapterType.MINISHIFT17, "1.22.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.22.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1230 (CDKServerAdapterType.MINISHIFT17, "1.23.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.23.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1240 (CDKServerAdapterType.MINISHIFT17, "1.24.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.24.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1250 (CDKServerAdapterType.MINISHIFT17, "1.25.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.25.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1261 (CDKServerAdapterType.MINISHIFT17, "1.26.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.26.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1270 (CDKServerAdapterType.MINISHIFT17, "1.27.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.27.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1280 (CDKServerAdapterType.MINISHIFT17, "1.28.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.28.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1290 (CDKServerAdapterType.MINISHIFT17, "1.29.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.29.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1300 (CDKServerAdapterType.MINISHIFT17, "1.30.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.30.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1310 (CDKServerAdapterType.MINISHIFT17, "1.31.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.31.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1320 (CDKServerAdapterType.MINISHIFT17, "1.32.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.32.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1330 (CDKServerAdapterType.MINISHIFT17, "1.33.0", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.33.0-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1341 (CDKServerAdapterType.MINISHIFT17, "1.34.1", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.34.1-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1342 (CDKServerAdapterType.MINISHIFT17, "1.34.2", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.34.2-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	MINISHIFT1343 (CDKServerAdapterType.MINISHIFT17, "1.34.3", CDKLabel.Server.MINISHIFT_SERVER_NAME, "minishift-1.34.3-" + CDKRuntimeOS.get().getRuntimeFullName() + getArch()),
	CRC100 (CDKServerAdapterType.CRC, "1.0.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.0.0" + getArch()),
	CRC110 (CDKServerAdapterType.CRC, "1.1.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.1.0" + getArch()),
	CRC120 (CDKServerAdapterType.CRC, "1.2.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.2.0" + getArch()),
	CRC130 (CDKServerAdapterType.CRC, "1.3.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.3.0" + getArch()),
	CRC140 (CDKServerAdapterType.CRC, "1.4.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.4.0" + getArch()),
	CRC150 (CDKServerAdapterType.CRC, "1.5.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.5.0" + getArch()),
	CRC160 (CDKServerAdapterType.CRC, "1.6.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.6.0" + getArch()),	
	CRC170 (CDKServerAdapterType.CRC, "1.7.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.7.0" + getArch()),
	CRC180 (CDKServerAdapterType.CRC, "1.8.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.8.0" + getArch()),
	CRC190 (CDKServerAdapterType.CRC, "1.9.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.9.0" + getArch()),	
	CRC1100 (CDKServerAdapterType.CRC, "1.10.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.10.0" + getArch()),
	CRC1110 (CDKServerAdapterType.CRC, "1.11.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.11.0" + getArch()),
	CRC1120 (CDKServerAdapterType.CRC, "1.12.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.12.0" + getArch()),
	CRC1130 (CDKServerAdapterType.CRC, "1.13.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.13.0" + getArch()),
	CRC1140 (CDKServerAdapterType.CRC, "1.14.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.14.0" + getArch()),
	CRC1150 (CDKServerAdapterType.CRC, "1.15.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.15.0" + getArch()),
	CRC1160 (CDKServerAdapterType.CRC, "1.16.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.16.0" + getArch()),
	CRC1170 (CDKServerAdapterType.CRC, "1.17.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.17.0" + getArch()),
	CRC1180 (CDKServerAdapterType.CRC, "1.18.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.18.0" + getArch()),
	CRC1190 (CDKServerAdapterType.CRC, "1.19.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.19.0" + getArch()),
	CRC1200 (CDKServerAdapterType.CRC, "1.20.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.20.0" + getArch()),
	CRC1210 (CDKServerAdapterType.CRC, "1.21.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.21.0" + getArch()),
	CRC1220 (CDKServerAdapterType.CRC, "1.22.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.22.0" + getArch()),
	CRC1230 (CDKServerAdapterType.CRC, "1.23.1", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.23.1" + getArch()),
	CRC1240 (CDKServerAdapterType.CRC, "1.24.0", CDKLabel.Server.CRC_SERVER_NAME, "crc-" + CDKRuntimeOS.get().getRuntimeFullName() + "-1.24.0" + getArch());

	
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
