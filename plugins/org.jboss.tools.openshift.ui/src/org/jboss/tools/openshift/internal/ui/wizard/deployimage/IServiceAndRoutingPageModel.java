/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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
 * @author Jeff Maury
 *
 */
public interface IServiceAndRoutingPageModel {

	static final String PROPERTY_ADD_ROUTE = "addRoute";

	String PROPERTY_SERVICE_PORTS = "servicePorts";
	String PROPERTY_SELECTED_SERVICE_PORT = "selectedServicePort";
	static final String PROPERTY_ROUTE_HOSTNAME = "routeHostname";
	String PROPERTY_ROUTING_PORT = "routingPort";
	
	boolean isAddRoute();
	
	void setAddRoute(boolean addRoute);
	
	List<IServicePort> getServicePorts();

	void setSelectedServicePort(IServicePort servicePort);

	IServicePort getSelectedServicePort();

	void removeServicePort(IServicePort port);

	/**
	 * Resets the model to expose all of the
	 * service ports that are listed by the 
	 * image;
	 */
	void resetServicePorts();
	
	void addServicePort(IServicePort port);
	
	void updateServicePort(IServicePort source, IServicePort target);
	
	/**
	 * Return the host name used assigned to route.
	 * 
	 * @return the host name assigned to route
	 */
	String getRouteHostname();
	
	/**
	 * Set the host name used by route.
	 * 
	 * @param routeHostname the route host name
	 */
	void setRouteHostname(String routeHostname);

        /**
	 * Set the port to be used for routing
	 *  
	 * @param port the port to use
	 */
	void setRoutingPort(IServicePort port);
	
	/**
	 * Return the port used for routing. May be null (round robin)
	 * 
	 * @return the routing port or null
	 */
	IServicePort getRoutingPort();
}
