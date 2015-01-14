/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.core.internal;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.tools.openshift.core.Connection;
import org.jboss.tools.openshift.core.ConnectionVisitor;
import org.jboss.tools.openshift.express.core.OpenShiftCoreException;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;

import com.openshift.kube.OpenShiftKubeClient;
import com.openshift.client.OpenShiftException;
import com.openshift.client.Refreshable;

public class KubernetesConnection extends OpenShiftKubeClient implements Connection, Refreshable {

	private String host;
	private String password;
	private String username;

	public KubernetesConnection(String host, String username, String password) throws MalformedURLException {
		super(new URL(host));
		this.host = host;
		this.username = username;
		this.password = password;
	}

	@Override
	public boolean connect() throws OpenShiftException {
		authorize();
		initializeCapabilities();
		return true;
	}

	@Override
	public void accept(ConnectionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public boolean isDefaultHost() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getScheme() {
		return UrlUtils.getScheme(host);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
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
		KubernetesConnection other = (KubernetesConnection) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public void refresh() {
		this.connect();
	}

	@Override
	public String toString() {
		try {
			return UrlUtils.getUrlFor(username, host);
		} catch (UnsupportedEncodingException e) {
			throw new OpenShiftCoreException(e, "Unable to determine URL from username: '%s', host: '%s'", username, host);
		}
	}
	
}
