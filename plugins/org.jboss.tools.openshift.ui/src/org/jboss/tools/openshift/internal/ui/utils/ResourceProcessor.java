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
package org.jboss.tools.openshift.internal.ui.utils;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * A Resource processor allows to plug resource specific actions.
 * 
 * @author Jeff Maury
 *
 */
public interface ResourceProcessor {
    /**
     * Return if delete cascade will delete linked resources.
     * 
     * @return false if cascade delete is a simple delete
     */
    default boolean willCascadeDeleteLinkedResources() {
        return false;
    }
    
    /**
     * Process the deletion of the resource.
     * 
     * @param connection the Openshift connection
     * @param resource the resource to delete
     * @param willDeleteSubResources if cascade delete
     */
    default void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource, boolean willDeleteSubResources) throws OpenShiftException {
        connection.deleteResource(resource);
        registry.fireConnectionChanged(connection, ConnectionProperties.PROPERTY_RESOURCE, resource, null);
    }
}
