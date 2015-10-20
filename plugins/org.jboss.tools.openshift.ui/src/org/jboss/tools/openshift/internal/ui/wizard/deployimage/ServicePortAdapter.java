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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import org.jboss.tools.common.databinding.ObservablePojo;

import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IServicePort;

/**
 * 
 * @author jeff.cantrill
 *
 */
class ServicePortAdapter extends ObservablePojo implements IServicePort{
	
	private String name;
	private int port;
	private int containerPort;
	private String protocol;

	ServicePortAdapter(IPort port){
		name = port.getName();
		this.port = port.getContainerPort();
		containerPort = port.getContainerPort();
		protocol = port.getProtocol();
	}
	
	ServicePortAdapter(IServicePort port) {
		this.name = port.getName();
		this.port = port.getPort();
		this.containerPort = port.getTargetPort() == 0 ? this.port : port.getTargetPort();
		this.protocol = port.getProtocol();
	}

	public ServicePortAdapter() {
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		firePropertyChange("name", this.name, this.name = name);
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void setPort(int port) {
		firePropertyChange("port", this.port, this.port = port);
	}

	@Override
	public int getTargetPort() {
		return containerPort;
	}

	@Override
	public void setTargetPort(int port) {
		firePropertyChange("targetPort", this.containerPort, this.containerPort = port);
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public void setProtocol(String proto) {
		firePropertyChange("protocol", this.protocol, this.protocol = proto);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
		result = prime * result + containerPort;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
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
		ServicePortAdapter other = (ServicePortAdapter) obj;
		if (port != other.port)
			return false;
		if (containerPort != other.containerPort)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		return true;
	}
	
	
}