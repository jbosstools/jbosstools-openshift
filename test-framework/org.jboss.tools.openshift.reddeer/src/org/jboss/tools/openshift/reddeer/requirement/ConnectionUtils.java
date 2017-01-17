/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;

/**
 * @author adietish@redhat.com
 */
public class ConnectionUtils {

	public static Connection getConnectionOrDefault(String connectionUrl) {
		if (!StringUtils.isBlank(connectionUrl)) {
			return getConnection(connectionUrl);
		} else {
			return getConnection(DatastoreOS3.USERNAME, DatastoreOS3.SERVER);
		}
	}
	
	/**
	 * Returns a connection for the given connection url (@see {@link ConnectionURL}. ex. https://adietish@10.1.2.2:8443).
	 * @param connectionUrlString
	 * @return
	 * 
	 * @see ConnectionURL
	 */
	public static Connection getConnection(String connectionUrlString) {
		try {
			ConnectionURL connectionUrl = ConnectionURL.forURL(connectionUrlString);
			return ConnectionsRegistrySingleton.getInstance().getByUrl(connectionUrl, Connection.class);
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not lookup connection {0}: {1}", connectionUrlString, e));
		}
	}

	/**
	 * Returns a connection for the given connection server and username
	 * @param connectionUrlString
	 * @return
	 * 
	 * @see ConnectionURL
	 */
	public static Connection getConnection(String username, String server) {
		try {
			String url = UrlUtils.getUrlFor(username, server);
			ConnectionURL connectionUrl = ConnectionURL.forURL(url);
			return ConnectionsRegistrySingleton.getInstance().getByUrl(connectionUrl, Connection.class);
		} catch (UnsupportedEncodingException | MalformedURLException e) {
			throw new OpenShiftToolsException(NLS.bind("Could not lookup connection to server {0}: {1}", server, e));
		}
	}

	public static void clearAll() {
		ConnectionsRegistrySingleton.getInstance().clear();
	}
}
