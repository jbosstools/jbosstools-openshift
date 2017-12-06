package org.jboss.tools.openshift.cdk.server.core.internal.detection;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CommandTimeoutException;

public class MinishiftVersionLoader {
	public static String ERROR_KEY = "properties.load.error";
	public static String VERSION_KEY = "Minishift version";
	public static String VERSION_KEY2 = "minishift";
	public static String CDK_VERSION_KEY = "CDK";
	public static String CDK_VERSION_KEY_OLD = "CDK Version";

	public static MinishiftVersions getVersionProperties(String homeDir) {
		Properties ret = new Properties();
		try {
			String[] lines = CDKLaunchUtility.call(homeDir, new String[] { "version" },
					new File(homeDir).getParentFile(), new HashMap<String, String>(), 5000, false);

			for (int i = 0; i < lines.length; i++) {
				if (lines[i].trim().isEmpty())
					continue;
				if (lines[i].contains(":")) {
					// Old format, Some Key: 1.2.3
					String[] split = lines[i].split(":");
					if (split.length == 2)
						ret.put(split[0], split[1]);
				} else if (lines[i].contains(" ")) {
					String[] split = lines[i].split(" ");
					if (split.length == 2)
						ret.put(split[0], split[1]);
				}
			}
		} catch (IOException | CommandTimeoutException e) {
			ret.put(ERROR_KEY, e.getMessage());
		}
		return new MinishiftVersions(ret);
	}

	public static class MinishiftVersions {
		private Properties p;

		public MinishiftVersions(Properties p) {
			this.p = p;
		}

		public String getError() {
			return p.getProperty(ERROR_KEY);
		}

		/**
		 * Get the minishift version
		 * 
		 * @return
		 */
		@Deprecated
		public String getVersion() {
			return getMinishiftVersion();
		}

		public String getMinishiftVersion() {
			String v = p.getProperty(VERSION_KEY);
			if (v == null) {
				v = p.getProperty(VERSION_KEY2);
			}
			return cleanVersion(v);
		}

		private String cleanVersion(String v) {
			if (v == null)
				return null;
			if (v.trim().startsWith("v")) {
				return v.trim().substring(1);
			}
			return v.trim();
		}

		public String getCDKVersion() {
			// CDK Version
			String v = p.getProperty(CDK_VERSION_KEY);
			if (v == null) {
				v = p.getProperty(CDK_VERSION_KEY_OLD);
			}
			return cleanVersion(v);
		}

		public boolean isValid() {
			return getMinishiftVersion() != null;
		}
	}
}
