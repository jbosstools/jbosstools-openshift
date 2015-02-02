/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;

import com.openshift.client.OpenShiftException;
import com.openshift.client.Refreshable;
//import com.openshift.kube.OpenShiftKubeClient;

public class Connection implements IConnection, Refreshable { //extends OpenShiftKubeClient implements IConnection, Refreshable {

	private String host;
//	private String password;
//	private String username;
	
	public Connection(String host){
		this.host = host;
	}
//	public Connection(String host, String username, String password) throws MalformedURLException {
//		super(new URL(host));
//		this.host = host;
//		this.username = username;
//		this.password = password;
//	}

//	@Override
	public boolean connect() throws OpenShiftException {
//		authorize();
//		initializeCapabilities();
		return true;
	}

	@Override
	public String getHost() {
		return host;
	}

//	@Override
//	public String getPassword() {
//		return password;
//	}
//
//	@Override
//	public String getUsername() {
//		return username;
//	}

	@Override
	public boolean isDefaultHost() {
		// TODO: implement
		return false;
	}

	@Override
	public String getScheme() {
		if (host.startsWith(UrlUtils.SCHEME_HTTPS)) {
			return UrlUtils.SCHEME_HTTPS;
		} else if (host.startsWith(UrlUtils.SCHEME_HTTP)) {
			return UrlUtils.SCHEME_HTTP;
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
//		result = prime * result
//				+ ((password == null) ? 0 : password.hashCode());
//		result = prime * result
//				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
//		if (password == null) {
//			if (other.password != null)
//				return false;
//		} else if (!password.equals(other.password))
//			return false;
//		if (username == null) {
//			if (other.username != null)
//				return false;
//		} else if (!username.equals(other.username))
//			return false;
		return true;
	}

	@Override
	public void refresh() {
		this.connect();
	}
	
//	@Override
	//TODO deleteme?
	public ConnectionType getType() {
		return ConnectionType.Kubernetes;
	}

	@Override
	public String toString() {
		return host;
	}
	
}
