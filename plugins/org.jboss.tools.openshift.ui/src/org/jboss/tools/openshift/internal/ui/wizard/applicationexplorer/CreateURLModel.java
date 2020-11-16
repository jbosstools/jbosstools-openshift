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

import java.util.List;

import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class CreateURLModel extends ComponentModel {
	public static final String PROPERTY_URL_NAME = "URLName";
	public static final String PROPERTY_PORT = "port";
	public static final String PROPERTY_SECURE = "secure";

	private String urlName;
	private int port = 1024;
	private List<Integer> ports;
	private boolean secure;

	public CreateURLModel(Odo odo, String projectName, String applicationName, String componentName,
			List<Integer> ports) {
		super(odo, projectName, applicationName, componentName);
		this.ports = ports;
		if (!ports.isEmpty()) {
			setPort(ports.get(0));
		}
	}

	/**
	 * @return the urlName
	 */
	public String getURLName() {
		return urlName;
	}

	/**
	 * @param urlName the urlName to set
	 */
	public void setURLName(String urlName) {
		firePropertyChange(PROPERTY_URL_NAME, this.urlName, this.urlName = urlName);
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		firePropertyChange(PROPERTY_PORT, this.port, this.port = port);
	}

	/**
	 * @return the ports
	 */
	public List<Integer> getPorts() {
		return ports;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		firePropertyChange(PROPERTY_SECURE, this.secure, this.secure = secure);
	}
}
