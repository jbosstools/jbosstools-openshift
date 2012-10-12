/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils.UrlPortions;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 */
public class ConnectionUtils {

	private ConnectionUtils() {
		// inhibit instantiation
	}

	public static String getUrlForUsername(String username) throws UnsupportedEncodingException, MalformedURLException {
		UrlPortions portions = UrlUtils.toPortions(getDefaultHostUrl());
		return UrlUtils.getUrlFor(username, portions.getHost(), portions.getProtocol());
	}

	/**
	 * Returns the default host from the preferences if present. If it's not it
	 * will return the host defined in the OpenShift configuration. The host
	 * that is returned will always have the scheme prefix.
	 * 
	 * @return the default host
	 */
	public static String getDefaultHostUrl() {
		try {
			String defaultHost = OpenShiftPreferences.INSTANCE.getDefaultHost();
			if (!StringUtils.isEmpty(defaultHost)) {
				return defaultHost;
			}
			return new OpenShiftConfiguration().getLibraServer();
		} catch (Exception e) {
			Logger.error("Could not load default server from OpenShift configuration.", e);
		}
		return null;
	}
}
