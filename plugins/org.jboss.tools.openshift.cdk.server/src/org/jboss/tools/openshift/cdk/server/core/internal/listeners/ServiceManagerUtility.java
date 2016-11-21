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
package org.jboss.tools.openshift.cdk.server.core.internal.listeners;

import java.util.HashMap;

public class ServiceManagerUtility {

	public static HashMap<String, String> parseLines(String[] lines) {
		HashMap<String, String> adbEnv = new HashMap<>();

		String setEnvWin = "setx ";
		String setEnvNix = "export ";

		for (String oneAppend : lines) {
			String[] allAppends = oneAppend.split("\n");
			for (int i = 0; i < allAppends.length; i++) {
				String setEnvVarCommand = null;
				String setEnvVarDelim = null;
				if (allAppends[i].trim().startsWith(setEnvWin)) {
					setEnvVarCommand = setEnvWin;
					setEnvVarDelim = " ";
				} else if (allAppends[i].trim().startsWith(setEnvNix)) {
					setEnvVarCommand = setEnvNix;
					setEnvVarDelim = "=";
				}
				if (setEnvVarCommand != null) {
					String lineRemainder = allAppends[i].trim().substring(setEnvVarCommand.length());
					int eq = lineRemainder.indexOf(setEnvVarDelim);
					if (eq != -1) {
						String k = lineRemainder.substring(0, eq);
						String v = lineRemainder.substring(eq + 1);
						if (v.startsWith("\"")) {
							v = v.substring(1);
						}
						if (v.endsWith("\"")) {
							v = v.substring(0, v.length() - 1);
						}
						adbEnv.put(k, v);
					}
				}
			}
		}

		if (adbEnv.size() > 0) {
			return adbEnv;
		}
		return null;
	}
}
