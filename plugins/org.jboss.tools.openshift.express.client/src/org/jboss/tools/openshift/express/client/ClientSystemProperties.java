/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.client;

import com.openshift.client.IHttpClient;
import com.openshift.internal.client.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class ClientSystemProperties {

	public static void setReadTimeoutSeconds(int seconds) {
		setSystemProperty(IHttpClient.SYSPROP_OPENSHIFT_READ_TIMEOUT, seconds * 1000);
	}
	
	public static int getReadTimeoutMillis() {
		return getSystemProperty(
				IHttpClient.SYSPROP_OPENSHIFT_READ_TIMEOUT, 0);
	}

	public static int getReadTimeoutSeconds() {
		return getReadTimeoutMillis() / 1000;
	}
	
	private static int getSystemProperty(String name, int defaultValue) {
		return toInteger(System.getProperty(name), defaultValue);
	}
	
	private static void setSystemProperty(String name, int value) {
		System.setProperty(name, String.valueOf(value));
	}
	
	private static int toInteger(String value, int defaultValue) {
		if (StringUtils.isEmpty(value)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}

}
