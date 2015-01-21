/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 * @author Jeff Cantrill
 */
public class ConnectionUtils {

	private ConnectionUtils() {
		// inhibit instantiation
	}
	
	public static Connection getConnectionByUrl(ConnectionURL connectionUrl, Map<ConnectionURL, Connection> connectionsByUrl) {
		if (connectionUrl == null) {
			return null;
		}
		return connectionsByUrl.get(connectionUrl);
	}
	
	/**
	 * Returns the connection for the given username if it exists. The
	 * connection must use the default host to match the query by username.
	 * 
	 * @param username
	 *            the username that the connection must use
	 * @return the connection with the given username that uses the default host
	 * 
	 * @see ConnectionUtils#getDefaultHostUrl()
	 */
	public static Connection getConnectionByUsername(String username, Map<ConnectionURL, Connection> connectionsByUrl) {
		try {
			return getConnectionByUrl(ConnectionURL.forUsername(username), connectionsByUrl);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		} catch (MalformedURLException e) {
			throw new OpenShiftCoreException(NLS.bind("Could not get url for connection {0}", username), e);
		}
	}
	
}
