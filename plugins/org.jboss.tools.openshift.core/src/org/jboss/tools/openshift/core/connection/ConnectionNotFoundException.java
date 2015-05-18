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
package org.jboss.tools.openshift.core.connection;

import org.jboss.tools.openshift.common.core.OpenShiftCoreException;

import com.openshift.restclient.model.IResource;

/**
 * An exception for when a connection can not be found for a given
 * resource
 * @author jeff.cantrill
 *
 */
public class ConnectionNotFoundException extends OpenShiftCoreException {

	public ConnectionNotFoundException(IResource resource) {
		super("Unable to find the connection for a %s named %s", resource.getKind(), resource.getName());
	}

	private static final long serialVersionUID = -1894208007989945899L;

}
