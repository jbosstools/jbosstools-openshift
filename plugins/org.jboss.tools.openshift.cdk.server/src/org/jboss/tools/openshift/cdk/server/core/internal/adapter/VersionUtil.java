package org.jboss.tools.openshift.cdk.server.core.internal.adapter;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class VersionUtil {
	private static final String MINISHIFT = "Minishift";
	private static final String CDK = "CDK";
	
	private VersionUtil() {
		// Intentionally blank 
	}
	
	public static boolean matchesAny(MinishiftVersions versions) {
		if( matchesCDK30(versions) == null || matchesCDK32(versions) == null 
				|| matchesMinishift17(versions) == null )
			return true;
		return false;
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
