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

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;

import com.openshift.restclient.model.IPort;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class PortSpecAdapter implements IPort{
	
	private String name;
	private String protocol;
	private int port;
	
	/**
	 * 
	 * @param spec A string in the form of "port/protocol"
	 * @throws IllegalArgumentException if the port info can not be determined from the spec
	 */
	public PortSpecAdapter(String spec){
		String[] parts = StringUtils.defaultIfBlank(spec, "").split("/");
		if(parts.length != 2){
			throw new IllegalArgumentException(NLS.bind("Unable to determine port info from spec {0}", spec));
		}
		port = Integer.valueOf(parts[0]);
		protocol = parts[1].toUpperCase(); 
		name = NLS.bind("{0}-{1}", port, protocol.toLowerCase());
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getContainerPort() {
		return port;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}
	
}