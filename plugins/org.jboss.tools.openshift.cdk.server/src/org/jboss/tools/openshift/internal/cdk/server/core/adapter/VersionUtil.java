/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader.MinishiftVersions;

public class VersionUtil {
	private static final String MINISHIFT = "Minishift";
	private static final String CDK = "CDK";
	private static final String CRC = "CRC";
	
	private VersionUtil() {
		// Intentionally blank 
	}
	
	public static boolean matchesCRC1(String version) {
		return version.startsWith("1.");
	}

	public static String matchesCRC10(MinishiftVersions versions) {
		String crcVers = versions.getCRCVersion();
		if (crcVers == null) {
			return cannotDetermine(CRC);
		}
		if (matchesCRC1(crcVers)) {
			return null;
		}
		return notCompatible(CRC, crcVers);
	}
	
	public static String matchesCDK32(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if (cdkVers != null) {
			if (CDK32Server.matchesCDK32(cdkVers)) {
				return null;
			}
			return notCompatible(CDK, cdkVers);
		}
		return cannotDetermine(CDK);
	}

	public static String matchesCDK30(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if (cdkVers == null) {
			return cannotDetermine(CDK);
		}
		if (CDK3Server.matchesCDK3(cdkVers)) {
			return null;
		}
		return notCompatible(CDK, cdkVers);
	}
	public static String matchesMinishift17(MinishiftVersions versions) {
		if( versions.getCDKVersion() != null ) {
			return notCompatible(CDK, versions.getCDKVersion());
		}

		String msVers = versions.getMinishiftVersion();
		if (msVers != null) {
			if (Minishift17Server.matchesMinishift17OrGreater(msVers)) {
				return null;
			}
			return notCompatible(MINISHIFT, msVers);
		}
		return cannotDetermine(MINISHIFT);
	}
	private static String cannotDetermine(String type) {
		return NLS.bind("Cannot determine {0} version.", type);
	}
	
	private static String notCompatible(String type, String vers) {
		return NLS.bind("{0} version {1} is not compatible with this server adapter.", type, vers);
	}
}
