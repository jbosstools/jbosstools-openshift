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
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.ConnectionType;
import org.jboss.tools.openshift.common.core.connection.IConnection;

import com.openshift.client.OpenShiftException;
import com.openshift.client.Refreshable;
import com.openshift3.client.IClient;
import com.openshift3.client.ResourceKind;
import com.openshift3.client.model.IResource;
import com.openshift3.internal.client.DefaultClient;

public class Connection  implements IConnection, Refreshable {

	private final IClient client;
	
	public Connection(String url) throws MalformedURLException{
		this(new DefaultClient(new URL(url)));
	}
	
	public Connection(IClient client){
		this.client = client;
	}

//	@Override
	public boolean connect() throws OpenShiftException {
//		authorize();
//		initializeCapabilities();
		return true;
	}

	@Override
	public String getHost() {
			return client.getBaseURL().getHost();
	}

	@Override
	public boolean isDefaultHost() {
		// TODO: implement
		return false;
	}

	@Override
	public String getScheme() {
		return client.getBaseURL().getProtocol();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((client  == null) ? 0 : client.getBaseURL().hashCode());
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
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.getBaseURL().toString().equals(other.client.getBaseURL().toString()))
			return false;
		return true;
	}

	@Override
	public void refresh() {
		this.connect();
	}
	
	@Override
	public ConnectionType getType() {
		return ConnectionType.Kubernetes;
	}

	@Override
	public String toString() {
		return client.getBaseURL().toString();
	}

	/**
	 * Get a list of resource types
	 * @return List<IResource>
	 */
	public <T extends IResource> List<T> get(ResourceKind kind) {
		return client.list(kind);
	}
	
}
