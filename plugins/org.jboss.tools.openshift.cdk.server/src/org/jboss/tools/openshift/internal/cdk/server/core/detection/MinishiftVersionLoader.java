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
package org.jboss.tools.openshift.internal.cdk.server.core.detection;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.CommandTimeoutException;
import org.jboss.tools.openshift.internal.cdk.server.core.adapter.controllers.ProcessLaunchUtility;

public class MinishiftVersionLoader {
	public static final String ERROR_KEY = "properties.load.error";
	public static final String VERSION_KEY = "Minishift version";
	public static final String VERSION_KEY2 = "minishift";
	public static final String CDK_VERSION_KEY = "CDK";
	public static final String CDK_VERSION_KEY_OLD = "CDK Version";
	public static final String CRC_VERSION_KEY = "CRC";
	public static final String CRC_VERSION_KEY_OLD = "CRC Version";
	
	public static MinishiftVersions getVersionProperties(String commandPath) {
		Properties ret = new Properties();
		try {
			String[] lines = ProcessLaunchUtility.call(commandPath, new String[] { "version" },
					new File(commandPath).getParentFile(), new HashMap<>(), 30000, false);

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
			if( v.contains("+")) {
				v = v.substring(0, v.indexOf("+"));
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

		public String getCRCVersion() {
			// there is another API change in CRC 1.12, from now on it is 
			// CodeReady Containers version: 1.12.0+6710aff
			String key = "crc version";
			if (p.containsKey("CodeReady Containers version")) {
				key = "CodeReady Containers version";
			} else if (p.containsKey("CRC version")) {
				key = "CRC version";
			}
			String v = p.getProperty(key);
			return cleanVersion(v);
		}
		
		public boolean isValid() {
			return getMinishiftVersion() != null;
		}
	}
}
