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
package org.jboss.tools.openshift.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Common attribute info about an openshift cluster
 * @author jeff.cantrill
 * @author Jeff Maury
 *
 */
public interface ICommonAttributes {

	/**
	 * The default 'library' namespace for the cluster
	 */
	static final String COMMON_NAMESPACE = "openshift";

	/**
     * The property key for saving the cluster namespace
     */
    static final String CLUSTER_NAMESPACE_KEY = "org.jbosstools.openshift.core.connection.ext.cluster.namespace";

	/**
	 * The property key for saving the image registry url
	 */
	static final String IMAGE_REGISTRY_URL_KEY = "org.jbosstools.openshift.core.connection.ext.registry.url";

	/**
	 * Human readable labels for extended properties
	 */
	@SuppressWarnings("serial")
	static final Map<String, String> EXTENDED_PROPERTY_LABELS = new HashMap<String, String>() {
		{
			put(CLUSTER_NAMESPACE_KEY, "Cluster Namespace");
			put(IMAGE_REGISTRY_URL_KEY, "Registry URL");
		}
	};

}
