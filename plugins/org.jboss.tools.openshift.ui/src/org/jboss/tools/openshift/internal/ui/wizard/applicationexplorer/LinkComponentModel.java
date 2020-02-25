/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.Odo;

import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 *
 */
public class LinkComponentModel extends LinkModel<Component> {
	public static final String PROPERTY_PORT = "port";
	public static final String PROPERTY_PORTS = "ports";
	
	private Integer port;
	
	private List<Integer> ports;
	private OpenShiftClient client;
	private Map<String, List<Integer>> cache = new HashMap<>();
	
	public LinkComponentModel(Odo odo, String projectName, String applicationName, String componentName, List<Component> targets, OpenShiftClient client) {
		super(odo, projectName, applicationName, componentName, targets);
		this.client = client;
	}

	/**
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(Integer port) {
		firePropertyChange(PROPERTY_PORT, this.port, this.port = port);
	}

	/**
	 * @return the ports
	 */
	public List<Integer> getPorts() {
		return ports;
	}

	/**
	 * @param ports the ports to set
	 */
	public void setPorts(List<Integer> ports) {
		this.ports = ports;
		if (!ports.isEmpty()) {
			setPort(ports.get(0));
		}
	}

	@Override
	public void setTarget(Component target) {
		super.setTarget(target);
		setPorts(cache.computeIfAbsent(target.getName(), name -> getOdo().getServicePorts(client, getProjectName(), getApplicationName(), target.getName())));
	}
}
