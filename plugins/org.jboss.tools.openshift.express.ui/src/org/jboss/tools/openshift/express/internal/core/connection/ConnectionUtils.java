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

import java.io.IOException;

import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

import com.openshift.client.configuration.IOpenShiftConfiguration;
import com.openshift.client.configuration.OpenShiftConfiguration;

/**
 * @author Andre Dietisheim
 */
public class ConnectionUtils {

	private ConnectionUtils() {
		// inhibit instantiation
	}

	/**
	 * Returns the default host from the preferences if present. If it's not it
	 * will return the host defined in the OpenShift configuration. The host
	 * that is returned will always have the scheme prefix.
	 * 
	 * @return the default host
	 * 
	 * @see OpenShiftPreferences#getDefaultHost()
	 * @see IOpenShiftConfiguration#getLibraServer()
	 */
	public static String getDefaultHostUrl() {
		try {
			String defaultHost = OpenShiftPreferences.INSTANCE.getDefaultHost();
			if (!StringUtils.isEmpty(defaultHost)) {
				return defaultHost;
			}
			return new OpenShiftConfiguration().getLibraServer();
		} catch (IOException e) {
			Logger.error("Could not load default server from OpenShift configuration.", e);
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if the given host is the default host. This
	 * method reports a given String is the default host if it is empty or if
	 * it's equal to the default host defined for this plugin. This plugin takes
	 * the default host from the preferences or the openshift configuration. If
	 * the given host has no scheme this method will assume it's https.
	 * 
	 * @param host
	 *            the host to check whether it is the default host
	 * @return true if it is equal to the default host
	 * 
	 * @see getDefaultHost()
	 */
	public static boolean isDefaultHost(String host) {
		return UrlUtils.isEmptyHost(host)
				|| getDefaultHostUrl().equals(
						UrlUtils.ensureStartsWithScheme(host, UrlUtils.SCHEME_HTTPS));
	}
}
