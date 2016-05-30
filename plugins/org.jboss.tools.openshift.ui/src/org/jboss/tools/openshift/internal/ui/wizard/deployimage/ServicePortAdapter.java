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
public class ServicePortAdapter extends ObservablePojo implements IServicePort{
	
	private static final String TARGET_PORT = "targetPort";
	private String name;
	private int port;
	private String containerPort;
	private String protocol;

	public ServicePortAdapter(IPort port){
		name = port.getName();
		this.port = port.getContainerPort();
		containerPort = String.valueOf(port.getContainerPort());
		protocol = port.getProtocol();
	}
	
	public ServicePortAdapter(IServicePort port) {
		this.name = port.getName();
		this.port = port.getPort();
		this.containerPort = "0".equals(port.getTargetPort()) ? String.valueOf(this.port) : port.getTargetPort();
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
	public String getTargetPort() {
		return containerPort;
	}

	@Override
	public void setTargetPort(int port) {
		firePropertyChange(TARGET_PORT, this.containerPort, this.containerPort = String.valueOf(port));
	}
	
	@Override
	public void setTargetPort(String intOrString) {
		firePropertyChange(TARGET_PORT, this.containerPort, this.containerPort = String.valueOf(intOrString));
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
		result = prime * result + ((containerPort == null) ? 0 : containerPort.hashCode());
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
		if (containerPort == null) {
			if (other.containerPort != null)
				return false;
		} else if (!containerPort.equals(other.containerPort))
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

    /**
     * For better test reporting
     */
    @Override
    public String toString() {
        return "ServicePortAdapter [name=" + name + ", port=" + port + ", containerPort=" + containerPort
                + ", protocol=" + protocol + "]";
    }
	
	
}
