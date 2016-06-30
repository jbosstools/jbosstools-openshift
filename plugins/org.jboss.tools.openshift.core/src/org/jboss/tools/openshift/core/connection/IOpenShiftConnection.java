/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.common.core.connection.IConnection;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Connection to a Kubernetes based version
 * of OpenShift
 * @author jeff.cantrill
 *
 */
public interface IOpenShiftConnection extends IConnection {

	static final String PROPERTY_EXTENDED_PROPERTIES = "extendedProperties";
	
	/**
	 * Retrieve a list of resources of the given kind;
	 * @param kind
	 * @return
	 * @throws OpenShiftException
	 */
	<T extends IResource> List<T> getResources(String kind);
	
	<T extends IResource> List<T> getResources(String kind, String namespace);

	<T extends IResource> T getResource(IResource resource);
	
	/**
	 * Retrieve a resource by name
	 * @param kind
	 * @param namespace
	 * @param name
	 * @return
	 */
	<T extends IResource> T getResource(String kind, String namespace, String name);
	
	String getUsername();
	
	/**
	 * Map of extended properties for
	 * a connection (e.g. public url to the registry
	 * @return
	 */
	Map<String, Object> getExtendedProperties();
	
	/**
	 * Set the extended properties for a connection.
	 * @param ext
	 */
	void setExtendedProperties(Map<String, Object> ext);
	
	/**
	 * Set a value of an extended property
	 * @param name
	 * @param value
	 */
	void setExtendedProperty(String name, Object value);
}
