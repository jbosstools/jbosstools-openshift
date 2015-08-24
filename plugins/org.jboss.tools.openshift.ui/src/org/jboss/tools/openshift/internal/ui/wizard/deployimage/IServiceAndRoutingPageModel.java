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

import java.util.List;

import com.openshift.restclient.model.IServicePort;

/**
 * 
 * @author jeff.cantrill
 *
 */
public interface IServiceAndRoutingPageModel {

	static final String PROPERTY_ADD_ROUTE = "addRoute";

	String PROPERTY_SERVICE_PORTS = "servicePorts";
	String PROPERTY_SELECTED_SERVICE_PORT = "selectedServicePort";
	
	boolean isAddRoute();
	
	void setAddRoute(boolean addRoute);
	
	List<IServicePort> getServicePorts();

	void setSelectedServicePort(IServicePort servicePort);

	IServicePort getSelectedServicePort();

	void removeServicePort(IServicePort port);

	/**
	 * Resets the model to expose all of the
	 * service ports that are lised by the 
	 * image;
	 */
	void resetServicePorts();

}
