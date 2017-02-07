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

	private static final String PREFIX_SET = "set ";
	private static final String PREFIX_SETX = "setx ";
	private static final String PREFIX_EXPORT = "export ";
	private static String getLineType(String line) {
		String trimmedLower = line.trim().toLowerCase();
		if( trimmedLower.startsWith(PREFIX_SET))
			return PREFIX_SET;
		if( trimmedLower.startsWith(PREFIX_SETX))
			return PREFIX_SETX;
		if( trimmedLower.startsWith(PREFIX_EXPORT))
			return PREFIX_EXPORT;
		return null;
	}
	
	private static String getDelim(String type) {
		if( type.startsWith(PREFIX_SET))
			return "=";
		if( type.startsWith(PREFIX_SETX))
			return " ";
		if( type.startsWith(PREFIX_EXPORT))
			return "=";
		return null;
	}
	
	
	public static HashMap<String, String> parseLines(String[] lines) {
		HashMap<String, String> adbEnv = new HashMap<>();
		for (String oneAppend : lines) {
			String[] allAppends = oneAppend.split("\n");
			for (int i = 0; i < allAppends.length; i++) {
				String setEnvVarCommand = getLineType(allAppends[i]);
				if( setEnvVarCommand != null ) {
					String setEnvVarDelim = getDelim(setEnvVarCommand);
					String lineRemainder = allAppends[i].trim().substring(setEnvVarCommand.length());
					int eq = lineRemainder.indexOf(setEnvVarDelim);
					if (eq != -1) {
						String k = lineRemainder.substring(0, eq);
						String v = lineRemainder.substring(eq + 1);
						if (v.startsWith("\"") && v.endsWith("\"")) {
							v = v.substring(1);
							v = v.substring(0, v.length() - 1);
						}
						adbEnv.put(k, v);
					}
				}
			}
		}
		return adbEnv.size() > 0 ? adbEnv : null;
	}
	
	
}
