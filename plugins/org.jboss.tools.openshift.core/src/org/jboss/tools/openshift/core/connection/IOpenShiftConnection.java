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

import com.openshift.restclient.model.IResource;

/**
 * Connection to a Kubernetes based version
 * of OpenShift
 * @author jeff.cantrill
 *
 */
public interface IOpenShiftConnection {

	/**
	 * Retrieve a list of resources of the given kind;
	 * @param kind
	 * @return
	 * @throws OpenShiftException
	 */
	<T extends IResource> List<T> getResources(String kind);
	
	<T extends IResource> List<T> getResources(String kind, String namespace);

	/**
	 * Retrieve a resource by name
	 * @param kind
	 * @param namespace
	 * @param name
	 * @return
	 */
	<T extends IResource> T getResource(String kind, String namespace, String name);
	
	String getUsername();
}
