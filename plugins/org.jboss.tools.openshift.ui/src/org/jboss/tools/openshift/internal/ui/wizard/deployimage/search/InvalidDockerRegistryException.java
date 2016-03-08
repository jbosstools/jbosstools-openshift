/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.wizard.deployimage.search;

import com.openshift.restclient.OpenShiftException;

/**
 * Custom {@link OpenShiftException} raised when the Docker Registry that is accessed is not compatible with the Docker Registry v1 API.
 */
public class InvalidDockerRegistryException extends OpenShiftException {
	
	/** Generated Serial Version UID */
	private static final long serialVersionUID = -3167694006460745225L;
	
	/** The URI of the Registry that was accessed. */
	private final String registryURI;
	
	/**
	 * Constructor
	 * @param registryURI the URI of the Registry that was accessed.
	 */
	public InvalidDockerRegistryException(final String registryURI) {
		super("Docker Registry at '{}' is incompatible with API v1", registryURI);
		this.registryURI = registryURI;
	}
	
	/**
	 * @return the URI of the Registry that was accessed.
	 */
	public String getRegistryURI() {
		return registryURI;
	}

}
